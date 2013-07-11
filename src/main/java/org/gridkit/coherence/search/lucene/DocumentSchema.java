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
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexableFieldType;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.gridkit.coherence.search.lucene.util.JavaSerializationSerializer;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ChainedExtractor;

/**
 * This class describes rules to construct {@link AnalyzedDocument} from
 * cache entry.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@Portable
public class DocumentSchema implements DocumentExtractor, Serializable {

	private static final long serialVersionUID = 20130602L;

	public static final String DOCUMENT_ID = "@doc-key";
	public static final LuceneAnalyzerProvider DEFAULT_ANALYZER = new ReflectionAnalyzerProvider(StandardAnalyzer.class, Version.LUCENE_42);
	
	@PortableProperty(value = 1, codec = JavaSerializationSerializer.Codec.class)
	private List<FieldInfo> fieldMap = new ArrayList<FieldInfo>();

	public DocumentSchema() {
	}
	
	public DocumentSchema(String field, ValueExtractor extractor) {
		addTokenizedField(field, extractor);
	}
	
	public FieldConfig configureField(String name) {
		for(FieldInfo i: fieldMap) {
			if (i.fieldName.equals(name)) {
				return newConfigurator(i.extractor);
			}
		}
		return null;
	}
	
	protected FieldConfig newConfigurator(FieldExtractor extractor) {
		if (extractor instanceof TokenizedFieldExtractor) {
			return new TokenizedFieldConfigurer((TokenizedFieldExtractor) extractor);
		}
		else {
			return new UnknownFieldConfigurer(extractor);
		}
	}

	public TokenizedField addTokenizedField(String name, ValueExtractor extractor) {
		addField(name, new TokenizedFieldExtractor(extractor));
		return (TokenizedField) configureField(name);
	}

	public TokenizedField addTokenizedField(String name, String field) {
		addField(name, new TokenizedFieldExtractor(new ChainedExtractor(field)));
		return (TokenizedField) configureField(name);
	}
		
	public void addField(String name, FieldExtractor extractor) {
		if (configureField(name) == null) {
			fieldMap.add(new FieldInfo(name, extractor));
		}
		else {
			throw new IllegalStateException("Field '" + name + "' is already added");
		}
	}
	
	@Override
	public AnalyzedDocument extract(final Object object) {
		Map.Entry<Object, Object> entry = new Map.Entry<Object, Object>() {
			@Override
			public Object getKey() {
				return null;
			}

			@Override
			public Object getValue() {
				return object;
			}

			@Override
			public Object setValue(Object value) {
				throw new UnsupportedOperationException();
			}
		};
		
		Map<String, IndexableField> fields = new LinkedHashMap<String, IndexableField>(fieldMap.size());
		
		for(FieldInfo fi: fieldMap) {
			IndexableField field = fi.extractor.extract(fi.fieldName, entry);
			if (field != null) {
				fields.put(fi.fieldName, field);
			}
		}
		
		return new AnalyzedDocument.SimpleDoc(fields);
	}
	
	public interface FieldConfig {
		
		public FieldExtractor getConfiguredExtractor();
		
	}
	
	public interface TokenizedField extends FieldConfig {
		
		public TokenizedField setAnalyzer(Class<? extends Analyzer> type);

		public TokenizedField setAnalyzer(LuceneAnalyzerProvider provider);
		
		public TokenizedField setIndexOptions(IndexOptions options);
		
		public TokenizedField setStoreTremVectors(boolean enabled);

		public TokenizedField setStoreTermVectorOffsets(boolean enabled);

		public TokenizedField setStoreTermVectorPositions(boolean enabled);

		public TokenizedField setStoreTermVectorPayloads(boolean enabled);
		
	}
	
	private static class FieldInfo implements Serializable {
		
		private static final long serialVersionUID = 20130602L;
		
		private String fieldName;
		private FieldExtractor extractor;

		public FieldInfo(String fieldName, FieldExtractor extractor) {
			this.fieldName = fieldName;
			this.extractor = extractor;
		}
	}
	
	public static interface FieldExtractor extends Serializable, Cloneable {

		public IndexableField extract(String fieldName, Map.Entry<Object, Object> entry);
		
		public FieldExtractor clone();
		
	}
	
//	@Override
//	@SuppressWarnings("unchecked")
//	public void readExternal(PofReader in) throws IOException {
//		fieldMap = (List<FieldInfo>) in.readCollection(1, new ArrayList<GenericFieldFactory>()); 
//	}
//
//	@Override
//	public void writeExternal(PofWriter out) throws IOException {
//		out.writeCollection(1, fieldMap);
//	}
//
	private static SerializeableFieldType DEFAULT_TOKENIZED_TYPE = new SerializeableFieldType();
	static {
		DEFAULT_TOKENIZED_TYPE.setIndexed(true);
		DEFAULT_TOKENIZED_TYPE.setTokenized(true);
		DEFAULT_TOKENIZED_TYPE.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
		DEFAULT_TOKENIZED_TYPE.setStoreTermVectors(true);
		DEFAULT_TOKENIZED_TYPE.setStoreTermVectorOffsets(true);
		DEFAULT_TOKENIZED_TYPE.setStoreTermVectorPositions(true);
		DEFAULT_TOKENIZED_TYPE.setStoreTermVectorPayloads(false);
		DEFAULT_TOKENIZED_TYPE.setOmitNorms(true);
		DEFAULT_TOKENIZED_TYPE.freeze();
	}
	
	private static class UnknownFieldConfigurer implements FieldConfig {
		
		private final FieldExtractor extractor;

		public UnknownFieldConfigurer(FieldExtractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public FieldExtractor getConfiguredExtractor() {
			return extractor.clone();
		}
	}
	
	private static class TokenizedFieldConfigurer implements TokenizedField {
		
		private final TokenizedFieldExtractor extractor;

		public TokenizedFieldConfigurer(TokenizedFieldExtractor extractor) {
			this.extractor = extractor;
		}

		@Override
		public FieldExtractor getConfiguredExtractor() {
			return extractor.clone();
		}

		@Override
		public TokenizedField setAnalyzer(Class<? extends Analyzer> type) {
			if (type == null) {
				throw new NullPointerException("Analyzer cannot be null");
			}
			extractor.analyzer = new ReflectionAnalyzerProvider(type, Version.LUCENE_42);
			return this;
		}

		@Override
		public TokenizedField setAnalyzer(LuceneAnalyzerProvider provider) {
			if (provider == null) {
				throw new NullPointerException("Analyzer cannot be null");
			}
			extractor.analyzer = provider;
			return this;
		}

		@Override
		public TokenizedField setIndexOptions(IndexOptions options) {
			SerializeableFieldType type = new SerializeableFieldType(extractor.fieldType);
			type.setIndexOptions(options);
			type.freeze();
			extractor.fieldType = type;
			return this;
		}

		@Override
		public TokenizedField setStoreTremVectors(boolean enabled) {
			SerializeableFieldType type = new SerializeableFieldType(extractor.fieldType);
			type.setStoreTermVectors(enabled);
			if (!enabled) {
				type.setStoreTermVectorOffsets(false);
				type.setStoreTermVectorPositions(false);
			}
			type.freeze();
			extractor.fieldType = type;
			return this;
		}

		@Override
		public TokenizedField setStoreTermVectorOffsets(boolean enabled) {
			SerializeableFieldType type = new SerializeableFieldType(extractor.fieldType);
			type.setStoreTermVectorOffsets(enabled);
			type.freeze();
			extractor.fieldType = type;
			return this;
		}

		@Override
		public TokenizedField setStoreTermVectorPositions(boolean enabled) {
			SerializeableFieldType type = new SerializeableFieldType(extractor.fieldType);
			type.setStoreTermVectorPositions(enabled);
			type.freeze();
			extractor.fieldType = type;
			return this;
		}

		@Override
		public TokenizedField setStoreTermVectorPayloads(boolean enabled) {
			SerializeableFieldType type = new SerializeableFieldType(extractor.fieldType);
			type.setStoreTermVectorPayloads(enabled);
			type.freeze();
			extractor.fieldType = type;
			return this;
		}
	}
	
	public static class TokenizedFieldExtractor implements FieldExtractor, Cloneable {
		
		private static final long serialVersionUID = 20130602L;
		
		private ValueExtractor extractor;
		private SerializeableFieldType fieldType = DEFAULT_TOKENIZED_TYPE;
		private LuceneAnalyzerProvider analyzer = DEFAULT_ANALYZER;
		
		public TokenizedFieldExtractor() {
			// for POF
		}
		
		public TokenizedFieldExtractor(ValueExtractor extractor) {
			this.extractor = extractor;
		}
		
		@Override
		public IndexableField extract(String fieldName, Entry<Object, Object> entry) {
			try {
				Object value = extractor.extract(entry.getValue());
				if (value != null) {
					String text = value.toString();
					TokenStream ts = analyzer.getAnalyzer().tokenStream(fieldName, new StringReader(text));
					FieldData fd = new FieldData(fieldName, fieldType, ts, 1);
					return fd;
				}
				else {
					return null;
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public TokenizedFieldExtractor clone() {
			try {
				return (TokenizedFieldExtractor) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new Error("Imposible");
			}
		}
	}

//	public static class NumberFieldExtractor implements FieldExtractor {
//		
//	}
//
//	public static class VerbatimFieldExtractor implements FieldExtractor {
//		
//	}
//
	private static class FieldData implements IndexableField {

		private String name;
		private IndexableFieldType fieldType;
		private float boost;
		private TokenStream tokenStream;

		public FieldData(String name, IndexableFieldType fieldType, TokenStream tokenStream, float boost) {
			this.name = name;
			this.fieldType = fieldType;
			this.tokenStream = tokenStream;
			this.boost = boost;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public IndexableFieldType fieldType() {
			return fieldType;
		}

		@Override
		public float boost() {
			return boost;
		}

		@Override
		public BytesRef binaryValue() {
			return null;
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
			return tokenStream;
		}
	}
	
	
}
