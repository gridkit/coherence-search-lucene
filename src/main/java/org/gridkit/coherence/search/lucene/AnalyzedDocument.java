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

import java.util.Map;

import org.apache.lucene.index.IndexableField;

/**
 * A collection of analyzed Lucene's {@link IndexableField}s.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public interface AnalyzedDocument {

	public Map<String, IndexableField> getFields();
	
	public static class SimpleDoc implements AnalyzedDocument {
		
		private final Map<String, IndexableField> fields;

		@SuppressWarnings("unchecked")
		public SimpleDoc(Map<String, ? extends IndexableField> fields) {
			this.fields = (Map<String, IndexableField>) fields;
		}

		@Override
		public Map<String, IndexableField> getFields() {
			return fields;
		}
	}
}
