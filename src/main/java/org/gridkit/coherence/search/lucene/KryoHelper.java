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

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;

class KryoHelper {

	public static Kryo getKryo() {
		Kryo k = new Kryo();
		k.setInstantiatorStrategy(LuceneInstantiatorStrategy.INSTANCE);
		return k;
	}

	private static class LuceneInstantiatorStrategy implements InstantiatorStrategy {

		static LuceneInstantiatorStrategy INSTANCE = new LuceneInstantiatorStrategy();
		
		private Map<Class<?>, ObjectInstantiator> instantiators = new HashMap<Class<?>, ObjectInstantiator>();
		{
			instantiators.put(BooleanClause.class, new ObjectInstantiator() {
				@Override
				public Object newInstance() {
					return new BooleanClause(null, null);
				}
			});
			instantiators.put(TermQuery.class, new ObjectInstantiator() {
				@Override
				public Object newInstance() {
					return new TermQuery(null);
				}
			});
			instantiators.put(Term.class, new ObjectInstantiator() {
				@Override
				public Object newInstance() {
					return new Term("");
				}
			});
			instantiators.put(PrefixQuery.class, new ObjectInstantiator() {
				@Override
				public Object newInstance() {
					return new PrefixQuery(new Term(""));
				}
			});
			instantiators.put(StandardAnalyzer.class, new ObjectInstantiator() {
				@Override
				public Object newInstance() {
					return new StandardAnalyzer(Version.LUCENE_42);
				}
			});
		}
		
		@Override
		@SuppressWarnings("rawtypes")
		public ObjectInstantiator newInstantiatorOf(Class type) {
			ObjectInstantiator oi = instantiators.get(type);
			if (oi == null) {
				throw new IllegalArgumentException(type.getName() + " has not instantiator");
			}
			return oi;
		}
	}
}
