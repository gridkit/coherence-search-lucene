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

import java.util.concurrent.Callable;

import org.gridkit.coherence.chtest.CacheConfig;
import org.gridkit.coherence.chtest.CacheConfig.DistributedScheme;
import org.gridkit.coherence.chtest.CacheConfig.LocalScheme;
import org.gridkit.coherence.chtest.CohCloud;
import org.gridkit.coherence.chtest.DisposableCohCloud;
import org.gridkit.coherence.chtest.PermCleaner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.filter.AlwaysFilter;

public abstract class LuceneSearchEndToEnd implements JAXBSearchTestSet {

	protected abstract JAXBSearchTestSet getJAXBSearchTarget();
	
	@BeforeClass
	public static void ensurePermSpace() {
		PermCleaner.forcePermSpaceGC(0.7);
	}	
	
	@Override
	@Test
	public void verify_standard_query_by_title() {
		getJAXBSearchTarget().verify_standard_query_by_title();		
	}

	@Override
	@Test
	public void verify_standard_query_by_text() {
		getJAXBSearchTarget().verify_standard_query_by_text();		
	}
	
	@Override
	@Test
	public void verify_standard_query_by_tag() {
		getJAXBSearchTarget().verify_standard_query_by_tag();		
	}
	
	

	@Override
	@Test
	public void verify_rest_query() {
		getJAXBSearchTarget().verify_rest_query();
	}

	public static void configureCommonCluster(CohCloud cloud) {
		// required for Coherence 12.1.2.0
		cloud.all().setProp("tangosol.coherence.cachefactory", "com.tangosol.net.DefaultConfigurableCacheFactory");
		
		cloud.all().presetFastLocalCluster();
		cloud.all().pofConfig("test-pof-config.xml");
		cloud.node("server.**").localStorage(true);
		cloud.node("client.**").localStorage(false);
	}

	public static abstract class AbstractCohCloudClientBasedTest extends LuceneSearchEndToEnd {

		@ClassRule
		public static DisposableCohCloud cloud = new DisposableCohCloud();
		
		@Override
		protected JAXBSearchTestSet getJAXBSearchTarget() {
			final String cacheName = "jaxb-test";
			cloud.all().getCache(cacheName);
			return cloud.node("client").exec(new Callable<JAXBSearchTestSet>() {
				@Override
				public JAXBSearchTestSet call() throws Exception {
					JAXBSearchTestSet.Body set = new JAXBSearchTestSet.Body();
					NamedCache cache = CacheFactory.getCache(cacheName);
					set.setCache(cache);
					if (System.getProperty("noindex") == null) {
						set.createIndex();
					}
					return set;
				}
			});
		}		
	}

	public static abstract class AbstractCohCloudCQCTest extends LuceneSearchEndToEnd {
		
		@ClassRule
		public static DisposableCohCloud cloud = new DisposableCohCloud();
		
		@Override
		protected JAXBSearchTestSet getJAXBSearchTarget() {
			final String cacheName = "jaxb-test";
			cloud.all().getCache(cacheName);
			return cloud.node("client").exec(new Callable<JAXBSearchTestSet>() {
				@Override
				public JAXBSearchTestSet call() throws Exception {
					JAXBSearchTestSet.Body set = new JAXBSearchTestSet.Body();
					NamedCache cache = CacheFactory.getCache(cacheName);
					cache = new ContinuousQueryCache(cache, AlwaysFilter.INSTANCE);
					set.setCache(cache);
					if (System.getProperty("noindex") == null) {
						set.createIndex();
					}
					return set;
				}
			});
		}		
	}
	
	public static class DistributedCache_SingleNode_IndexOn_POF_Test extends AbstractCohCloudClientBasedTest {

		@BeforeClass
		public static void initCacheConfig() {
			DistributedScheme scheme = CacheConfig.distributedSheme();
			scheme.backingMapScheme(CacheConfig.localScheme());

			configureCommonCluster(cloud);
			cloud.all().pofEnabled(true);
			cloud.all().mapCache("*", scheme);
			cloud.nodes("server", "client");
		}
	}

	public static class DistributedCache_SingleNode_IndexOn_JavaSer_Test extends AbstractCohCloudClientBasedTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			DistributedScheme scheme = CacheConfig.distributedSheme();
			scheme.backingMapScheme(CacheConfig.localScheme());
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(false);
			cloud.all().mapCache("*", scheme);
			cloud.nodes("server", "client");
		}
	}

	public static class DistributedCache_SingleNode_IndexOff_POF_Test extends AbstractCohCloudClientBasedTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			DistributedScheme scheme = CacheConfig.distributedSheme();
			scheme.backingMapScheme(CacheConfig.localScheme());
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(true);
			cloud.all().mapCache("*", scheme);
			cloud.all().setProp("noindex", "true");
			cloud.nodes("server", "client");
		}
	}
	
	public static class DistributedCache_SingleNode_IndexOff_JavaSer_Test extends AbstractCohCloudClientBasedTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			DistributedScheme scheme = CacheConfig.distributedSheme();
			scheme.backingMapScheme(CacheConfig.localScheme());
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(false);
			cloud.all().mapCache("*", scheme);
			cloud.all().setProp("noindex", "true");
			cloud.nodes("server", "client");
		}
	}

	public static class LocalCache_IndexOn_Test extends AbstractCohCloudClientBasedTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			LocalScheme scheme = CacheConfig.localScheme();
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(false);
			cloud.all().mapCache("*", scheme);
			cloud.nodes("server", "client");
		}
	}
	
	public static class LocalCache_IndexOff_Test extends AbstractCohCloudClientBasedTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			LocalScheme scheme = CacheConfig.localScheme();
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(false);
			cloud.all().mapCache("*", scheme);
			cloud.all().setProp("noindex", "true");
			cloud.nodes("server", "client");
		}
	}

	public static class Distributed_CQCCache_IndexOn_Test extends AbstractCohCloudCQCTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			DistributedScheme scheme = CacheConfig.distributedSheme();
			scheme.backingMapScheme(CacheConfig.localScheme());
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(false);
			cloud.all().mapCache("*", scheme);
			cloud.nodes("server", "client");
		}
	}
	
	public static class Distributed_CQCCache_IndexOff_Test extends AbstractCohCloudCQCTest {
		
		@BeforeClass
		public static void initCacheConfig() {
			DistributedScheme scheme = CacheConfig.distributedSheme();
			scheme.backingMapScheme(CacheConfig.localScheme());
			
			configureCommonCluster(cloud);
			cloud.all().pofEnabled(false);
			cloud.all().mapCache("*", scheme);
			cloud.all().setProp("noindex", "true");
			cloud.nodes("server", "client");
		}
	}
}
