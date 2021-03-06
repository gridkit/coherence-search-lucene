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

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

public interface PartitionedIndexProcessor<S1, S2> {

	public static final String DOCUMENT_KEY = LuceneInMemoryIndex.DOCUMENT_KEY;
	
	public S2 executeOnResults(Collection<S1> nodeResults);
	
	public S1 executeOnIndex(IndexAggregationContext context) throws IOException;
	
	public static interface IndexAggregationContext {

		public Object docToKey(Document doc);

		public Object docIdToKey(int docId);

		public IndexSearcher getIndexSearcher();
		
	}
}
