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

import java.util.Collection;
import java.util.concurrent.Callable;

import org.gridkit.coherence.chtest.CacheConfig;
import org.gridkit.coherence.chtest.PermCleaner;
import org.gridkit.coherence.chtest.CacheConfig.LocalScheme;
import org.gridkit.coherence.chtest.CohCloud.CohNode;
import org.gridkit.coherence.chtest.DisposableCohCloud;
import org.gridkit.vicluster.ViNodeSet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

public class LocalLuceneNoIndexTest extends LuceneGenericFuntionalTestSet.Proxy {

	@ClassRule
	public static DisposableCohCloud cloud = new DisposableCohCloud();

	@BeforeClass
	public static void ensurePermSpace() {
		PermCleaner.forcePermSpaceGC(0.7);
	}		
	
	@BeforeClass
	public static void initCacheConfig() {
		LocalScheme scheme = CacheConfig.localScheme();
		cloud.all().mapCache("*", scheme);
		// required for Coherence 12.1.2.0
		cloud.all().setProp("tangosol.coherence.cachefactory", DefaultConfigurableCacheFactory.class.getName());
	}
	
	@Before
	public void init() {
		testSet = cloud.node("local").exec(new Callable<LuceneGenericFuntionalTestSet>() {
			@Override
			public LuceneGenericFuntionalTestSet call() throws Exception {
				LuceneGenericFuntionalTestSet.Body set = new LuceneGenericFuntionalTestSet.Body();
				NamedCache cacheA = CacheFactory.getCache("test-A");
				NamedCache cacheB = CacheFactory.getCache("test-B");
				set.setCacheA(cacheA);
				set.setCacheB(cacheB);
				return set;
			}
		});
	}

	public Statement apply(Statement base, Description description) {
		return cloud.apply(base, description);
	}

	public int hashCode() {
		return cloud.hashCode();
	}

	public ViNodeSet getCloud() {
		return cloud.getCloud();
	}

	public CohNode all() {
		return cloud.all();
	}

	public CohNode node(String namePattern) {
		return cloud.node(namePattern);
	}

	public CohNode nodes(String... namePatterns) {
		return cloud.nodes(namePatterns);
	}

	public Collection<CohNode> listNodes(String namePattern) {
		return cloud.listNodes(namePattern);
	}

	public void shutdown() {
		cloud.shutdown();
	}

	public boolean equals(Object obj) {
		return cloud.equals(obj);
	}

	public String toString() {
		return cloud.toString();
	}
}
