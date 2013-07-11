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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.util.BytesRef;
import org.gridkit.coherence.search.SearchFactory;
import org.gridkit.coherence.search.SearchFactory.SearchIndexCallable;
import org.gridkit.coherence.search.SearchFactory.SearchIndexWrapper;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap.EntryAggregator;
import com.tangosol.util.InvocableMap.ParallelAwareAggregator;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

public class LuceneIndexProcessorAgent implements ParallelAwareAggregator, PortableObject {

	private static final long serialVersionUID = 20110823L;
	
	private PartitionedIndexProcessor<?, ?> indexProcessor;
	private ValueExtractor indexExtractor;
	
	public LuceneIndexProcessorAgent(PartitionedIndexProcessor<?, ?> indexProcessor, ValueExtractor indexExtractor) {
		this.indexProcessor = indexProcessor;
		this.indexExtractor = indexExtractor;
	}
	
	@Override
	@SuppressWarnings("rawtypes")
	public Object aggregate(Set entries) {
		throw new UnsupportedOperationException();
	}

	@Override
	public EntryAggregator getParallelAggregator() {
		return new NodeAggregatorAgent(indexProcessor, indexExtractor);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object aggregateResults(Collection results) {
		Set pr = new HashSet(results);
		pr.remove(null);
		return indexProcessor.executeOnResults(results);
	}
	
	@Override
	public void readExternal(PofReader reader) throws IOException {
		this.indexProcessor = (PartitionedIndexProcessor<?, ?>) reader.readObject(1);
		this.indexExtractor = (ValueExtractor) reader.readObject(2);			
	}

	@Override
	public void writeExternal(PofWriter writer) throws IOException {
		writer.writeObject(1, indexProcessor);
		writer.writeObject(2, indexExtractor);			
	}	

	public static class NodeAggregatorAgent implements EntryAggregator, PortableObject {
		
		private static final long serialVersionUID = 20110823L;
		
		private PartitionedIndexProcessor<?, ?> indexProcessor;		
		private ValueExtractor indexExtractor;
		private transient boolean completed = false;

		// PortableObject no-arg constructor
		public NodeAggregatorAgent() {};
		
		public NodeAggregatorAgent(PartitionedIndexProcessor<?, ?> indexProcessor, ValueExtractor indexExtractor) {
			this.indexProcessor = indexProcessor;
			this.indexExtractor = indexExtractor;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public synchronized Object aggregate(Set entries) {
			if (entries.isEmpty() || completed) {
				return null;
			}
			else {
				Map.Entry<?, ?> e = (Entry<?, ?>) entries.iterator().next();
				if (e instanceof BinaryEntry) {
					BinaryEntry be = (BinaryEntry)e;
					Map<ValueExtractor,MapIndex> indexMap = be.getBackingMapContext().getIndexMap();
					MapIndex mi =indexMap.get(indexExtractor);
					if (mi != null) {
						@SuppressWarnings("unchecked")
						IndexContext ctx = new IndexContext((SearchIndexWrapper<LuceneInMemoryIndex>)mi);
						try {							
							Object pr = indexProcessor.executeOnIndex(ctx);
							return pr;
						}
						catch(IOException ex) {
							throw new RuntimeException(ex);
						}
						finally {
							ctx.close();
							completed = true;
						}
					}
					else {
						throw new UnsupportedOperationException("No Lucene index found");
					}
				}
				else {
					throw new UnsupportedOperationException("Can be invoked only on partitioned cache");
				}
			}
		}

		@Override
		public void readExternal(PofReader reader) throws IOException {
			this.indexProcessor = (PartitionedIndexProcessor<?, ?>) reader.readObject(1);
			this.indexExtractor = (ValueExtractor) reader.readObject(2);			
		}

		@Override
		public void writeExternal(PofWriter writer) throws IOException {
			writer.writeObject(1, indexProcessor);
			writer.writeObject(2, indexExtractor);			
		}
	}
	
	private static class IndexContext implements PartitionedIndexProcessor.IndexAggregationContext {
		
		private SearchFactory.SearchIndexWrapper<LuceneInMemoryIndex> index;
		private IndexSearcher searcher;
		
		public IndexContext(SearchIndexWrapper<LuceneInMemoryIndex> index) {
			this.index = index;
			SearchIndexCallable<LuceneInMemoryIndex, IndexSearcher> callable = new SearchIndexCallable<LuceneInMemoryIndex, IndexSearcher>() {				
				@Override
				public IndexSearcher execute(LuceneInMemoryIndex li) {
					return li.getSearcher();
				}
			};			
			this.searcher = index.callCoreIndex(callable);			
		}

		@Override
		public Object docIdToKey(int docId) {
			try {
				Document doc = searcher.doc(docId, Collections.singleton(LuceneInMemoryIndex.DOCUMENT_KEY));
				BytesRef br = doc.getBinaryValue(LuceneInMemoryIndex.DOCUMENT_KEY);
				Object key = index.ensureObjectKey(new Binary(br.bytes, br.offset, br.length));
				return key;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Object docToKey(Document doc) {
			BytesRef br = doc.getBinaryValue(LuceneInMemoryIndex.DOCUMENT_KEY);
			Object key = index.ensureObjectKey(new Binary(br.bytes, br.offset, br.length));
			return key;
		}

		@Override
		public IndexSearcher getIndexSearcher() {
			return searcher;
		}
		
		public void close() {
			// do nothing;
		}
	}
}
