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


import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.gridkit.coherence.search.lucene.rest.StandardRestQueryParser;
import org.gridkit.coherence.search.lucene.xml.JAXBSchemaAdapter;
import org.junit.Assert;
import org.junit.Ignore;

import com.tangosol.net.NamedCache;

import example.Ticket;
import example.TicketLoader;

public interface JAXBSearchTestSet extends Remote {

	public void verify_standard_query_by_title();

	public void verify_rest_query();

	public void verify_standard_query_by_text();

	public void verify_standard_query_by_tag();
	

	@Ignore
	public static class Body implements JAXBSearchTestSet {
		
		protected NamedCache cache;

		protected JAXBSchemaAdapter jaxbSchema = new JAXBSchemaAdapter();
		protected LuceneSearchFactory jaxbSearchFactory = new LuceneSearchFactory(jaxbSchema);

		public void setCache(NamedCache cache) {
			this.cache = cache;
		}

		public void createIndex() {
			jaxbSearchFactory.createIndex(cache);
		}
		
		void initSmallDataSet() {
			cache.clear();
			add(TicketLoader.loadTickets("tickets/small-set.xml"));
			
		}

		void add(Ticket ticket) {
			cache.put(ticket.getId(), ticket);
		}

		void add(Collection<Ticket> list) {
			for(Ticket tt: list) {
				cache.put(tt.getId(), tt);
			}
		}
				
		@Override
		public void verify_standard_query_by_title() {
			initSmallDataSet();
			String[] ids = searchIds("title:SOS");
			Assert.assertArrayEquals(new String[]{"T-2"}, ids);
		}

		@Override
		public void verify_standard_query_by_text() {
			initSmallDataSet();
			Assert.assertArrayEquals(new String[]{"T-1"}, searchIds("text:Something"));
			Assert.assertArrayEquals(new String[]{"T-1"}, searchIds("text:some*"));
		}

		@Override
		public void verify_standard_query_by_tag() {
			initSmallDataSet();
			String[] ids = searchIds("tags.tag:REALLY-BAD");
			Assert.assertArrayEquals(new String[]{"T-1"}, ids);
		}

		@Override
		public void verify_rest_query() {
			initSmallDataSet();

			String[] ids;
			ids = restQuery("tags.tag:REALLY-BAD");
			Assert.assertArrayEquals(new String[]{"T-1"}, ids);
			
			ids = restQuery("REALLY-BAD");
			Assert.assertArrayEquals(new String[]{"T-1"}, ids);

			ids = restQuery("big bad");
			Assert.assertArrayEquals(new String[]{"T-1"}, ids);

			ids = restQuery("trouble");
			Assert.assertArrayEquals(new String[]{"T-1", "T-2"}, ids);

			ids = restQuery("tags.tag:$tags", "tags=REALLY-BAD GRAVE-DANGER");
			Assert.assertArrayEquals(new String[]{"T-1", "T-2"}, ids);
			
		}

//		private Ticket[] search(String query) {
//			try {
//				StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer(Version.LUCENE_42));
//				return search(parser.parse(query, "description"));
//			} catch (QueryNodeException e) {
//				throw new RuntimeException(e);
//			}
//		}
//
		private String[] searchIds(String query) {
			try {
				StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer(Version.LUCENE_42));
				Ticket[] tt = search(parser.parse(query, "description"));
				String[] ids = new String[tt.length];
				for(int i = 0; i != tt.length; ++i) {
					ids[i] = tt[i].getId();
				}
				return ids;
			} catch (QueryNodeException e) {
				throw new RuntimeException(e);
			}
		}

		private String[] restQuery(String query, String... params) {
			Map<String, Object> pm = new HashMap<String, Object>();
			for(String param: params) {
				pm.put(param.substring(0, param.indexOf('=')), param.substring(param.indexOf('=') + 1));
			}
			StandardRestQueryParser parser = new StandardRestQueryParser();
			Ticket[] tt = search(parser.parse(query, pm));
			String[] ids = new String[tt.length];
			for(int i = 0; i != tt.length; ++i) {
				ids[i] = tt[i].getId();
			}
			return ids;
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Ticket[] search(Query query) {
			Set entries = cache.entrySet(jaxbSearchFactory.createFilter(query));
			List<Ticket> result = new ArrayList<Ticket>();
			for(Map.Entry entry: (Set<Map.Entry>)entries) {
				result.add((Ticket) entry.getValue());
			}
			
			return result.toArray(new Ticket[0]);		
		}		
	}
}
