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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.tangosol.net.NamedCache;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.extractor.ReflectionExtractor;

public interface LuceneGenericFuntionalTestSet extends Remote {

	public void test_combinatoric_query32();

	public void test_combinatoric_query128();

	public void test_combinatoric_query512();

	public void test_combinatoric_query1024();

	public void test_combinatoric_query2048();

	public void test_combinatoric_query4096();

	public void verify_query_all();
	
	public void verify_tag_query_and_update();

	@Ignore
	public static class Body implements LuceneGenericFuntionalTestSet {
		
	    private static final String[] WORD_SET = {
	    	"red", "blue", "yellow", "grey", "green", 
	    	"purple", "white", "cyan", "magenta", "pink", "brown", "navy"
	    };
	
		
		protected NamedCache cacheA;
		protected NamedCache cacheB;
		protected DocumentSchema dumbDocSchema = new DocumentSchema("text", new ReflectionExtractor("getTitle"));
		protected LuceneSearchFactory dumbFactory = new LuceneSearchFactory(dumbDocSchema);

		protected DocumentSchema complexDocSchema = new DocumentSchema();
		{
			complexDocSchema.addTokenizedField("id", "getId");
			complexDocSchema.addTokenizedField("project", "getProject");
			complexDocSchema.addTokenizedField("title", "getTitle");
			complexDocSchema.addTokenizedField("description", "getDescription");
			complexDocSchema.addTokenizedField("tag", "getTagLine").setAnalyzer(WhitespaceAnalyzer.class).setIndexOptions(IndexOptions.DOCS_ONLY).setStoreTremVectors(false);
		}
		
		protected LuceneSearchFactory compexFactory = new LuceneSearchFactory(complexDocSchema);
	
		public void setCacheA(NamedCache cache) {
			this.cacheA = cache;
		}

		public void setCacheB(NamedCache cache) {
			this.cacheB = cache;
		}
		
		public void createIndex() {
			dumbFactory.createIndex(cacheA);
			compexFactory.createIndex(cacheB);
		}
		
		void init_combinatoric_test_data(int objectCount) {
	
			cacheA.clear();
			
	        long rangeStart = 0;
	        long rangeFinish = rangeStart + objectCount;
			
	        int putSize = 10;
	        for(long i = rangeStart;  i < rangeFinish; i += putSize) {
	            long j = Math.min(rangeFinish, i + putSize);
	            cacheA.putAll(generateEncodedNumber(i, j, WORD_SET));
	        }
		}

		void init_tickets() {
			cacheB.clear();
			add(loadTickets("tickets/LUCENE-4.4-TICKETS.xml"));
		}

		void add(Issue issue) {
			cacheB.put(issue.id, issue);
		}

		void add(Collection<Issue> list) {
			for(Issue tt: list) {
				cacheB.put(tt.id, tt);
			}
		}
		
		Issue newTicket(String id, String title, String project, String descr, String... tags) {
			Issue tt = new Issue();
			tt.id = id;
			tt.title = title;
			tt.project = project;
			tt.description = descr;
			tt.tags = Arrays.asList(tags);
			return tt;
		}

		
		@SuppressWarnings("unchecked")
		static List<Issue> loadTickets(String file) {

			List<Issue> tickets = new ArrayList<Issue>();

			XmlElement xml = XmlHelper.loadFileOrResource(file, "\"Trouble ticket\" test data");
			
			xml = xml.getElement("channel");
			for(XmlElement item: (List<XmlElement>)xml.getElementList()) {
				if ("item".equals(item.getName())) {
					Issue tt = new Issue();
					tt.id = item.getSafeElement("key").getString();
					tt.title = item.getSafeElement("title").getString();
					tt.project = item.getSafeElement("project").getAttribute("key").getString();
					tt.description = item.getSafeElement("description").getString();
					List<String> tags = new ArrayList<String>();
					for(XmlElement tag: (List<XmlElement>)item.getSafeElement("labels").getElementList()) {
						if ("label".equals(tag.getName())) {
							tags.add(tag.getString());
						}
					}
					tt.tags = tags;
					tickets.add(tt);
				}
			}
			return tickets;
		}
		
	    static Map<Object, Object> generateEncodedNumber(long from, long to, String[] wordset) {
	        Map<Object, Object> result = new HashMap<Object, Object>();
	        for (long i = from; i != to; ++i) {
	            Object text = encodeBitPositions(i, wordset);
	            result.put(String.valueOf(i), text);
	        }
	        return result;
	    }    
	    
	    static Object encodeBitPositions(long value, String[] charset) {
	        StringBuilder builder = new StringBuilder();
	        for(int i =0 ; i != charset.length; ++i) {
	            if ((value & (1 << i)) != 0) {
	            	if (builder.length() > 0) {
	            		builder.append(" ");
	            	}
	                builder.append(charset[i]);
	            }
	        }
	        Issue ticket = new Issue();
	        ticket.title = builder.toString();
	        return ticket;
	    }
	    
		@Override
		public void test_combinatoric_query32() {
			init_combinatoric_test_data(32);
			Assert.assertEquals(32, cacheA.size());
			Assert.assertEquals(2, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2], WORD_SET[3]).length);
			Assert.assertEquals(4, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2]).length);
			Assert.assertEquals(8, dumbQuery(WORD_SET[0], WORD_SET[1]).length);
			Assert.assertEquals(16, dumbQuery(WORD_SET[0]).length);
			Assert.assertEquals(32, dumbQuery(new MatchAllDocsQuery()).length);
		}
	
		@Override
		public void test_combinatoric_query128() {
			init_combinatoric_test_data(128);
			Assert.assertEquals(128, cacheA.size());
			Assert.assertEquals(8, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2], WORD_SET[3]).length);
			Assert.assertEquals(16, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2]).length);
			Assert.assertEquals(32, dumbQuery(WORD_SET[0], WORD_SET[1]).length);
			Assert.assertEquals(64, dumbQuery(WORD_SET[0]).length);
			Assert.assertEquals(128, dumbQuery(new MatchAllDocsQuery()).length);
		}
	
		@Override
		public void test_combinatoric_query512() {
			init_combinatoric_test_data(512);
			Assert.assertEquals(512, cacheA.size());
			Assert.assertEquals(32, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2], WORD_SET[3]).length);
			Assert.assertEquals(64, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2]).length);
			Assert.assertEquals(128, dumbQuery(WORD_SET[0], WORD_SET[1]).length);
			Assert.assertEquals(256, dumbQuery(WORD_SET[0]).length);
			Assert.assertEquals(512, dumbQuery(new MatchAllDocsQuery()).length);
		}
	
		@Override
		@Test
		public void test_combinatoric_query1024() {
			init_combinatoric_test_data(1024);
			Assert.assertEquals(1024, cacheA.size());
			Assert.assertEquals(64, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2], WORD_SET[3]).length);
			Assert.assertEquals(128, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2]).length);
			Assert.assertEquals(256, dumbQuery(WORD_SET[0], WORD_SET[1]).length);
			Assert.assertEquals(512, dumbQuery(WORD_SET[0]).length);
			Assert.assertEquals(1024, dumbQuery(new MatchAllDocsQuery()).length);
		}
	
		@Override
		@Test
		public void test_combinatoric_query2048() {
			init_combinatoric_test_data(2048);
			Assert.assertEquals(2048, cacheA.size());
			Assert.assertEquals(128, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2], WORD_SET[3]).length);
			Assert.assertEquals(256, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2]).length);
			Assert.assertEquals(512, dumbQuery(WORD_SET[0], WORD_SET[1]).length);
			Assert.assertEquals(1024, dumbQuery(WORD_SET[0]).length);
			Assert.assertEquals(2048, dumbQuery(new MatchAllDocsQuery()).length);
		}
	
		@Override
		@Test
		public void test_combinatoric_query4096() {
			init_combinatoric_test_data(4096);
			Assert.assertEquals(4096, cacheA.size());
			Assert.assertEquals(256, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2], WORD_SET[3]).length);
			Assert.assertEquals(512, dumbQuery(WORD_SET[0], WORD_SET[1], WORD_SET[2]).length);
			Assert.assertEquals(1024, dumbQuery(WORD_SET[0], WORD_SET[1]).length);
			Assert.assertEquals(2048, dumbQuery(WORD_SET[0]).length);			
			Assert.assertEquals(4096, dumbQuery(new MatchAllDocsQuery()).length);
		}
		
		@Override
		@Test
		public void verify_query_all() {
			init_tickets();
			Issue[] result = issueSearch("*:*");
			Assert.assertEquals(234, result.length);
		}

		@Override
		@Test
		public void verify_tag_query_and_update() {
			init_tickets();

			Issue[] result = issueSearch("tag:a*");
			Assert.assertEquals(4, result.length);
			
			result[0].tags = new ArrayList<String>();
			cacheB.remove(result[0].id);
			add(result[0]);

			Issue[] result2 = issueSearch("tag:a*");
			Assert.assertEquals(3, result2.length);
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private String[] dumbQuery(Query query) {
			Set entries = cacheA.entrySet(dumbFactory.createFilter(query));
			List<String> result = new ArrayList<String>();
			for(Map.Entry entry: (Set<Map.Entry>)entries) {
				result.add(((Issue) entry.getValue()).getTitle());
			}
			
			return result.toArray(new String[0]);		
		}

		private Issue[] issueSearch(String query) {
			try {
				StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer(Version.LUCENE_42));
				return issueSearch(parser.parse(query, "description"));
			} catch (QueryNodeException e) {
				throw new RuntimeException(e);
			}
		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private Issue[] issueSearch(Query query) {
			Set entries = cacheB.entrySet(compexFactory.createFilter(query));
			List<Issue> result = new ArrayList<Issue>();
			for(Map.Entry entry: (Set<Map.Entry>)entries) {
				result.add((Issue) entry.getValue());
			}
			
			return result.toArray(new Issue[0]);		
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		private String[] dumbQuery(String... terms) {
			BooleanQuery query = new BooleanQuery();
			for(String term: terms) {
				TermQuery tq = new TermQuery(new Term("text", term));
				query.add(tq, Occur.MUST);
			}
			Set entries = cacheA.entrySet(dumbFactory.createFilter(query));
			List<String> result = new ArrayList<String>();
			for(Map.Entry entry: (Set<Map.Entry>)entries) {
				result.add(((Issue) entry.getValue()).getTitle());
			}
			
			return result.toArray(new String[0]);
		}
	}
	
	@Ignore
	public static class Proxy implements LuceneGenericFuntionalTestSet {
		
		protected LuceneGenericFuntionalTestSet testSet;

		
		@Test
		public void test_combinatoric_query32() {
			testSet.test_combinatoric_query32();
		}

		@Test
		public void test_combinatoric_query128() {
			testSet.test_combinatoric_query128();
		}

		@Test
		public void test_combinatoric_query512() {
			testSet.test_combinatoric_query512();
		}

		@Test
		public void test_combinatoric_query1024() {
			testSet.test_combinatoric_query1024();
		}

		@Test
		public void test_combinatoric_query2048() {
			testSet.test_combinatoric_query2048();
		}

		@Test
		public void test_combinatoric_query4096() {
			testSet.test_combinatoric_query4096();
		}

		@Test
		public void verify_query_all() {
			testSet.verify_query_all();
		}

		@Test
		public void verify_tag_query_and_update() {
			testSet.verify_tag_query_and_update();
		}				
	}
}
