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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.junit.Test;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

public class TokenStreamCheck {

	@Test
	public void analyze() throws IOException {
		
		WhitespaceAnalyzer wa = new WhitespaceAnalyzer(Version.LUCENE_42);
		wa.getOffsetGap("xxx");
		TokenStream ts = wa.tokenStream("test", new StringReader("red black tree"));
		ts.reset();
		ts.incrementToken();
		ts.getAttribute(CharTermAttribute.class).buffer();
		
		CapturedTokenStream cts = new CapturedTokenStream(ts);
		cts.reset();
		cts.incrementToken();
		cts.getAttribute(CharTermAttribute.class).buffer();
	}
	
	@Test
	public void loadTickets() {
		loadTickets("tickets/LUCENE-4877.xml");
		loadTickets("tickets/LUCENE-4.4-TICKETS.xml");
	}
	
	@SuppressWarnings("unchecked")
	public List<Issue> loadTickets(String file) {

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
}
