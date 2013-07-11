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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.gridkit.coherence.search.lucene.AnalyzedDocument;
import org.gridkit.coherence.search.lucene.DocumentExtractor;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

public class JAXBSchemaAdapter implements DocumentExtractor, PortableObject, Serializable {

	private static final long serialVersionUID = 20130621L;
	
	private static ConcurrentMap<Class<?>, Marshaller> MARSHALERS = new ConcurrentHashMap<Class<?>, Marshaller>(16, 0.7f, 4);
	private static DocumentBuilder DOC_BUILDER = createBuilder();

	private static DocumentBuilder createBuilder() {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		}
	} 
	
	private DocumentExtractor xmlSchema;
	
	public JAXBSchemaAdapter() {
		xmlSchema = new NaiveXmlSchema();
	}
	
	@Override
	public AnalyzedDocument extract(Object object) {
		try {
			Class<?> c = object.getClass();
			Marshaller m = MARSHALERS.get(c);
			if (m == null) {
				m = JAXBContext.newInstance(c).createMarshaller();
				MARSHALERS.putIfAbsent(c, m);
				m = MARSHALERS.get(c);
			}
			
			Document doc = DOC_BUILDER.newDocument();
			Node node = doc.createElement("root");
			m.marshal(object, node);			
			
			return xmlSchema.extract(node.getFirstChild());
		} catch (DOMException e) {
			throw new RuntimeException(e);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void readExternal(PofReader in) throws IOException {
		int id = 0;
		xmlSchema = (DocumentExtractor) in.readObject(++id);
		
	}

	@Override
	public void writeExternal(PofWriter out) throws IOException {
		int id = 0;
		out.writeObject(++id, xmlSchema);
	}
}
