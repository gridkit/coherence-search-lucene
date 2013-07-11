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
package org.gridkit.coherence.search.lucene.xml;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.gridkit.coherence.search.lucene.AnalyzedDocument;
import org.junit.Assert;
import org.junit.Test;

import example.Ticket;
import example.TicketLoader;

public class JAXBSchemaTest {

	@Test
	public void test_document_extraction() {
		Ticket t = TicketLoader.loadSmallTicketSet().get(0);
		JAXBSchemaAdapter adapter = new JAXBSchemaAdapter();
		AnalyzedDocument doc = adapter.extract(t);
		Set<String> fields = new HashSet<String>(Arrays.asList(
				"id",
				"reporter",
				"status",
				"text",
				"timestamp",
				"title",
				"tags.tag",
				"comments.comment.author",
				"comments.comment.text",
				"comments.comment.timestamp",
				"__text"
		));
		Assert.assertEquals(fields, doc.getFields().keySet());
	}
	
	@Test
	public void test_id_term_query() {
		Ticket t = new Ticket();
		t.setId("TICKET-1");
		Assert.assertTrue(matchQuery(t, parseStd("id:TICKET-1")));
	}

	@Test
	public void test_id_prefix_query() {
		Ticket t = new Ticket();
		t.setId("TICKET-1");
		Assert.assertTrue(matchQuery(t, parseStd("id:TIC*")));
	}

	@Test
	public void test_tag_term_query() {
		Ticket t = new Ticket();
		t.setId("TICKET-1");
		t.addTags("ABC", "CBD");
		Assert.assertTrue(matchQuery(t, parseStd("tags.tag:ABC")));
		Assert.assertTrue(matchQuery(t, parseStd("tags.tag:CBD")));
	}

	@Test
	public void test_small_set_tag_term_query() {
		Ticket t = TicketLoader.loadSmallTicketSet().get(0);
		Assert.assertTrue(matchQuery(t, parseStd("tags.tag:TROUBLE")));
		Assert.assertTrue(matchQuery(t, parseStd("tags.tag:REALLY-BAD")));
	}

	@Test
	public void test_small_set_text_query() {
		Ticket t = TicketLoader.loadSmallTicketSet().get(0);
		Assert.assertTrue(matchQuery(t, parseStd("Something")));
		Assert.assertTrue(matchQuery(t, parseStd("happened bad")));
		Assert.assertTrue(matchQuery(t, parseStd("some*")));
	}
	
	public Query parseStd(String query) {
		try {
			StandardQueryParser parser = new StandardQueryParser(new StandardAnalyzer(Version.LUCENE_42));
			return parser.parse(query, "text");
		} catch (QueryNodeException e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean matchQuery(Object object, Query query) {
		try {
			IndexSearcher is = newSearcher(object);
			return is.search(query, 1).totalHits > 0;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public IndexSearcher newSearcher(Object object) {
		try {
			RAMDirectory dir = new RAMDirectory();
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_42, new WhitespaceAnalyzer(Version.LUCENE_42));
			IndexWriter iw = new IndexWriter(dir, iwc);
			JAXBSchemaAdapter adapter = new JAXBSchemaAdapter();
			AnalyzedDocument doc = adapter.extract(object);
			iw.addDocument(doc.getFields().values());
			iw.commit();
			DirectoryReader reader = DirectoryReader.open(iw, true);
			return new IndexSearcher(reader);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
	}	
}
