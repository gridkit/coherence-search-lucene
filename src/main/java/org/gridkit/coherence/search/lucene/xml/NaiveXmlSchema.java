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
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.Version;
import org.gridkit.coherence.search.lucene.AnalyzedDocument;
import org.gridkit.coherence.search.lucene.CapturedTokenStream;
import org.gridkit.coherence.search.lucene.DocumentExtractor;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

public class NaiveXmlSchema implements DocumentExtractor, Serializable, PortableObject {

	public static final String WHOLE_TEXT = "__text";

	private static final long serialVersionUID = 20130623L;
	
	private static Analyzer DEFAULT_ANALYZER = new StandardAnalyzer(Version.LUCENE_42); 
	
	private Analyzer analyzer;
	private int childGap = 10000;
	
	public NaiveXmlSchema() {
		this.analyzer = null;
	}

	public NaiveXmlSchema(Analyzer analyzer) {
		this.analyzer = analyzer;
	}
	
	@Override
	public AnalyzedDocument extract(Object object) {
		try {
			Node node = (Node) object;
			
			Map<String, CapturedTokenStream> doc = new HashMap<String, CapturedTokenStream>();
			captureNode("", 0, node, doc);
			
			Map<String, IndexableField> fields = new HashMap<String, IndexableField>();
			for(String key: doc.keySet()) {
				fields.put(key, new TextField(key, doc.get(key)));
			}
			
			return new AnalyzedDocument.SimpleDoc(fields);
		} catch (DOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private long captureNode(String prefix, long globalPos, Node node, Map<String, CapturedTokenStream> doc) throws DOMException, IOException {
		for(Element n: textElements(node)) {
			addText(prefix, globalPos, n, doc);
		}		
		for(Element n: compositeElements(node)) {			
			globalPos = captureNode(prefix + n.getNodeName() + ".", globalPos + childGap, n, doc);
		}	
		return globalPos;
	}
	
	private void addText(String prefix, long globalPos, Element n, Map<String, CapturedTokenStream> doc) throws DOMException, IOException {
		String name = prefix + n.getNodeName();
		// TODO this will work incorrectly for nested elements
		TokenStream ts = getAnalyzer().tokenStream(name, new StringReader(n.getTextContent()));
		
		// adding to specific field index
		CapturedTokenStream cs = doc.get(name);
		if (cs == null) {
			cs = new CapturedTokenStream(ts, (int)globalPos, 0);
			doc.put(name, cs);
		}
		else {
			cs.append(ts, (int)(Math.max(0, globalPos - cs.getLastPosition())), 0);
		}
		
		// adding to whole text field
		// token stream have to be recreated
		ts = getAnalyzer().tokenStream(name, new StringReader(n.getTextContent()));
		CapturedTokenStream wt = doc.get(WHOLE_TEXT);
		if (wt == null) {
			wt = new CapturedTokenStream(ts, (int)globalPos, 0);
			doc.put(WHOLE_TEXT, wt);
		}
		else {
			wt.append(ts, (int)(Math.max(0, globalPos - cs.getLastPosition())), 0);
		}		
	}

	private Analyzer getAnalyzer() {
		return analyzer == null ? DEFAULT_ANALYZER : analyzer;
	}

	private List<Element> textElements(Node node) {
		List<Element> result = new ArrayList<Element>();
		NodeList list = node.getChildNodes();
		for(int i = 0; i != list.getLength(); ++i) {
			Node ch = list.item(i);
			if (ch instanceof Element) {
				if (hasText(ch)) {
					result.add((Element) ch);
				}
			}			
		}
		return result;
	}

	private List<Element> compositeElements(Node node) {
		List<Element> result = new ArrayList<Element>();
		NodeList list = node.getChildNodes();
		for(int i = 0; i != list.getLength(); ++i) {
			Node ch = list.item(i);
			if (ch instanceof Element) {
				if (hasElements(ch)) {
					result.add((Element) ch);
				}
			}			
		}
		return result;
	}

	private boolean hasText(Node node) {
		NodeList list = node.getChildNodes();
		for(int i = 0; i != list.getLength(); ++i) {
			Node ch = list.item(i);
			if (ch instanceof Text) {
				return true;
			}			
		}
		return false;
	}

	private boolean hasElements(Node node) {
		NodeList list = node.getChildNodes();
		for(int i = 0; i != list.getLength(); ++i) {
			Node ch = list.item(i);
			if (ch instanceof Element) {
				return true;
			}			
		}
		return false;
	}

	@Override
	public void readExternal(PofReader in) throws IOException {
		int id = 0;
		
		analyzer = (Analyzer) in.readObject(++id);
		childGap = in.readInt(++id);
	}

	@Override
	public void writeExternal(PofWriter out) throws IOException {
		int id = 0;
		
		out.writeObject(++id, analyzer);
		out.writeInt(++id, childGap);		
	}
}
