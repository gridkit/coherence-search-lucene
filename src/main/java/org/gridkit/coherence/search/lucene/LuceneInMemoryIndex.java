/**
 * Copyright 2013 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gridkit.coherence.search.lucene;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.gridkit.coherence.search.IndexInvocationContext;
import org.gridkit.coherence.search.IndexUpdateEvent;
import org.gridkit.coherence.search.IndexUpdateEvent.Type;

import com.tangosol.util.Binary;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
class LuceneInMemoryIndex {

	public static final String DOCUMENT_KEY = "@doc-key";
	private static final FieldType DOCUMENT_KEY_TYPE = new FieldType();
	static {
		DOCUMENT_KEY_TYPE.setIndexed(true);
		DOCUMENT_KEY_TYPE.setStored(true);
		DOCUMENT_KEY_TYPE.setOmitNorms(true);
		DOCUMENT_KEY_TYPE.freeze();
	}
	
	
	private Directory storage;
	
	private IndexWriter writer;
	private IndexSearcher searcher;
	
	public LuceneInMemoryIndex(Directory directory, Analyzer defaultAnalyzer) {
		this.storage = directory;
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_42, defaultAnalyzer);
		try {
			writer = new IndexWriter(storage, iwc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public synchronized void update(Map<Object, IndexUpdateEvent> events, IndexInvocationContext ctx) {
		try {
			if (searcher != null) {
				searcher = null;
			}

			try {
				for(IndexUpdateEvent event: events.values()) {
					if (event.getType() == Type.NOPE) {
						continue;
					}
					
					BytesRef br = toBytesRef(ctx.ensureBinaryKey(event.getKey()).toByteArray());
					
					switch(event.getType()) {
						case UPDATE:
							writer.deleteDocuments(new Term(DOCUMENT_KEY, br));
							writer.commit();
						case INSERT:
							AnalyzedDocument doc = (AnalyzedDocument)event.getValue();
							List<IndexableField> fields = makeDoc(br, doc);
							writer.addDocument(fields);
							break;
						case DELETE:
							writer.deleteDocuments(new Term(DOCUMENT_KEY, br));
							writer.commit();
					}
				}
			}
			finally {
//				writer.optimize();
//				writer.close();
				writer.commit();
				searcher = new IndexSearcher(DirectoryReader.open(writer, true));
			}
			
		} catch (IOException e) {
			// should not happen with RAMDirectory
			throw new RuntimeException(e);
		}
	}

	public synchronized void applyIndex(Query query, final Set<Object> keySet, final IndexInvocationContext context) {
		if (searcher == null) {
			// index is empty
			keySet.clear();
			return;
		}
		final Set<Object> retained = new HashSet<Object>();		
		try {
			searcher.search(query, new Collector() {
				
				IndexReader reader;
				
				@Override
				public void setScorer(Scorer scorer) throws IOException {
					// ignore
				}
				
				@Override
				public void setNextReader(AtomicReaderContext context)	throws IOException {
					this.reader = context.reader();
				}

				@Override
				public void collect(int doc) throws IOException {
					Document document = reader.document(doc, Collections.singleton(LuceneInMemoryIndex.DOCUMENT_KEY));
					BytesRef binKey = document.getBinaryValue(LuceneInMemoryIndex.DOCUMENT_KEY);
					Binary bin = new Binary(binKey.bytes, binKey.offset, binKey.length);
					Object key = context.ensureFilterCompatibleKey(bin);
					if (keySet.contains(key)) {
						retained.add(key);
					}
				}
				
				@Override
				public boolean acceptsDocsOutOfOrder() {
					return true;
				}
			});
		} catch (IOException e) {
			// should never happen with RAMDirectory
			throw new RuntimeException(e);
		}
		keySet.retainAll(retained);
	}	
	
    public synchronized IndexSearcher getSearcher() {
		try {
			DirectoryReader reader = DirectoryReader.open(storage);
			IndexSearcher searcher = new IndexSearcher(reader);
			return searcher;
		} catch (CorruptIndexException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
    
	private List<IndexableField> makeDoc(BytesRef binKey, AnalyzedDocument document) {
		List<IndexableField> fields = new ArrayList<IndexableField>();
		fields.add(new DocKey(binKey));
		for(IndexableField f: document.getFields().values()) {
			fields.add(f);
		}
		return fields;
	}

    private BytesRef toBytesRef(byte[] bytes) {
    	return new BytesRef(bytes);
    }

    private static class DocKey implements IndexableField {

    	private final BytesRef binkey;

		public DocKey(BytesRef binkey) {
			this.binkey = binkey;
		}

		@Override
		public String name() {
			return DOCUMENT_KEY;
		}

		@Override
		public IndexableFieldType fieldType() {
			return DOCUMENT_KEY_TYPE;
		}

		@Override
		public float boost() {
			return 1;
		}

		@Override
		public BytesRef binaryValue() {
			return binkey;
		}

		@Override
		public String stringValue() {
			return null;
		}

		@Override
		public Reader readerValue() {
			return null;
		}

		@Override
		public Number numericValue() {
			return null;
		}

		@Override
		public TokenStream tokenStream(Analyzer analyzer) throws IOException {
			return new DocKeyStream(binkey);
		}
    }
    
    private static class BinaryTokenAttribute extends AttributeImpl implements TermToBytesRefAttribute {

    	private BytesRef bytes;
    	
		public BinaryTokenAttribute(BytesRef bytes) {
			this.bytes = bytes;
		}

		@Override
		public int fillBytesRef() {
			return bytes.hashCode();
		}

		@Override
		public BytesRef getBytesRef() {
			return bytes;
		}

		@Override
		public void clear() {
		}

		@Override
		public void copyTo(AttributeImpl target) {
			((BinaryTokenAttribute)target).bytes = bytes;
		}
    }
    
    private static class DocKeyStream extends TokenStream {

    	private boolean started;
    	private boolean done;
    	
		public DocKeyStream(BytesRef bytes) {
			BinaryTokenAttribute token = new BinaryTokenAttribute(bytes);
			addAttributeImpl(token);
		}

		@Override
		public void reset() throws IOException {
			started = true;
			done = false;
		}

		@Override
		public boolean incrementToken() throws IOException {
			if (started && !done) {
				done = true;
				return true;
			}
			else {
				return false;
			}
		}
    }
}
