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

import org.apache.lucene.search.Query;
import org.gridkit.coherence.search.SearchFactory;

import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;

public class LuceneSearchFactory extends SearchFactory<LuceneInMemoryIndex, LuceneIndexConfig, Query> {

	public LuceneSearchFactory(DocumentExtractor luceneExtractor) {
		super(new LuceneSearchPlugin(), new DefaultLuceneIndexConfig(), luceneExtractor);
	}

	public void setLuceneIndexConfig(LuceneIndexConfig config) {
		indexConfig = config;
	}

	@Override
	public Filter createFilter(Query query) {
		return new LuceneQueryFilter(createFilterExtractor(), query);
	}
	
	public <K, V> ScoredEntries<K, V> search(NamedCache cache, Query query, int docLimit) {
		LuceneTopDocSearch processor = new LuceneTopDocSearch(query, docLimit);
		@SuppressWarnings("unchecked")
		ScoredEntries<K,V> entries = broadcast(cache, processor);
		return entries;
	}

	public <S1, S2> S2 broadcast(NamedCache cache, PartitionedIndexProcessor<S1, S2> indexProcessor) {
		LuceneIndexProcessorAgent agent = new LuceneIndexProcessorAgent(indexProcessor, createFilterExtractor());
		if (cache.getCacheService() instanceof DistributedCacheService || cache.getCacheService() instanceof RemoteCacheService) {
			@SuppressWarnings("unchecked")
			S2 result = (S2) cache.aggregate(AlwaysFilter.INSTANCE, agent);
			return result;
		}
		else {
			// TODO support for non distributed caches via filter
			throw new UnsupportedOperationException();
		}
	}
 }
