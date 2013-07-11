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

import junit.framework.Assert;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

/**
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class POFSerializationTest {

	
	private static ConfigurablePofContext context;
	
	@BeforeClass
	public static void init() {
		context = new ConfigurablePofContext("serialization-test-pof-config.xml");
	}
	
	@Test()
	public void test_LuceneQueryFilter() throws IllegalArgumentException, IllegalAccessException {
		PhraseQuery query = new PhraseQuery();
		query.add(new Term("abc", "123"));
		LuceneQueryFilter lqf = new LuceneQueryFilter(null, query);
		
		Binary bin = ExternalizableHelper.toBinary(lqf, context);
		LuceneQueryFilter lqf2 = (LuceneQueryFilter) ExternalizableHelper.fromBinary(bin, context);
		
		ReflectionComparator rcmp = new ReflectionComparator();
		
		Assert.assertTrue(rcmp.equivalent(lqf, lqf2));
	}

	@Test
	public void test_DefaultLuceneIndexConfig() throws IllegalArgumentException, IllegalAccessException {

		DefaultLuceneIndexConfig dc = new DefaultLuceneIndexConfig();
		
		Binary bin = ExternalizableHelper.toBinary(dc, context);
		DefaultLuceneIndexConfig dc2 = (DefaultLuceneIndexConfig) ExternalizableHelper.fromBinary(bin, context);
		
		ReflectionComparator rcmp = new ReflectionComparator();
		
		Assert.assertTrue(rcmp.equivalent(dc, dc2));
	}

	@Test
	public void test_GenericNamedLuceneIndexToken() throws IllegalArgumentException, IllegalAccessException {
		
		GenericNamedLuceneIndexToken token = new GenericNamedLuceneIndexToken("token2");
		
		Binary bin = ExternalizableHelper.toBinary(token, context);
		GenericNamedLuceneIndexToken token2 = (GenericNamedLuceneIndexToken) ExternalizableHelper.fromBinary(bin, context);
		
		ReflectionComparator rcmp = new ReflectionComparator();
		
		Assert.assertTrue(rcmp.equivalent(token, token2));
	}
	
	@Test
	public void test_DocumentSchema() throws IllegalArgumentException, IllegalAccessException {
		DocumentSchema extractor = new DocumentSchema();
		extractor.addTokenizedField("bin", new ReflectionExtractor("getX"));
		extractor.addTokenizedField("text", new ReflectionExtractor("getText"));
		
		Binary bin = ExternalizableHelper.toBinary(extractor, context);
		DocumentSchema extractor2 = (DocumentSchema) ExternalizableHelper.fromBinary(bin, context);
		
		ReflectionComparator rcmp = new ReflectionComparator();
		
		Assert.assertTrue(rcmp.equivalent(extractor, extractor2));		
	}

	@Test
	public void test_FactoryIndexExtractor() throws IllegalArgumentException, IllegalAccessException {
		
		DocumentSchema extractor = new DocumentSchema();
		extractor.addTokenizedField("bin", new ReflectionExtractor("getX"));
		extractor.addTokenizedField("text", new ReflectionExtractor("getText"));
		TestLuceneSearchFactory factory = new TestLuceneSearchFactory(extractor);
		
		ValueExtractor ie = factory.createConfiguredExtractor();
		
		Binary bin = ExternalizableHelper.toBinary(ie, context);
		ValueExtractor ie2 = (ValueExtractor) ExternalizableHelper.fromBinary(bin, context);
		
		ReflectionComparator rcmp = new ReflectionComparator();
		
		Assert.assertTrue(rcmp.equivalent(ie, ie2));		
	}

	@Test
	public void test_FactoryFilterExtractor() throws IllegalArgumentException, IllegalAccessException {
		
		DocumentSchema extractor = new DocumentSchema();
		extractor.addTokenizedField("bin", new ReflectionExtractor("getX"));
		extractor.addTokenizedField("text", new ReflectionExtractor("getText"));
		TestLuceneSearchFactory factory = new TestLuceneSearchFactory(extractor);
		
		ValueExtractor ie = factory.createFilterExtractor();
		
		Binary bin = ExternalizableHelper.toBinary(ie, context);
		ValueExtractor ie2 = (ValueExtractor) ExternalizableHelper.fromBinary(bin, context);
		
		ReflectionComparator rcmp = new ReflectionComparator();
		
		Assert.assertTrue(rcmp.equivalent(ie, ie2));		
	}
	
	public static class TestLuceneSearchFactory extends LuceneSearchFactory {

		public TestLuceneSearchFactory(DocumentSchema luceneExtractor) {
			super(luceneExtractor);
		}

		@Override
		protected SearchIndexExtractor<LuceneInMemoryIndex, LuceneIndexConfig, Query> createConfiguredExtractor() {
			return super.createConfiguredExtractor();
		}

		@Override
		protected SearchIndexExtractor<LuceneInMemoryIndex, LuceneIndexConfig, Query> createFilterExtractor() {
			return super.createFilterExtractor();
		}
	}
}
