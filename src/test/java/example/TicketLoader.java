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
package example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import com.tangosol.run.xml.XmlHelper;

public class TicketLoader {

	public static List<Ticket> loadTickets(String path) {
		try {
			StringReader sr = new StringReader(XmlHelper.loadFileOrResource(path, "Canned ticket").toString());
			Unmarshaller m = JAXBContext.newInstance(TicketList.class).createUnmarshaller();
			TicketList t = (TicketList) m.unmarshal(sr);
			return t.tickets;
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Ticket> loadSmallTicketSet() {
		return loadTickets("tickets/small-set.xml");
	}
	
	public static void storeTickets(String path, Ticket... tickets) throws JAXBException, IOException {
		TicketList tl = new TicketList();
		tl.tickets.addAll(Arrays.asList(tickets));
		Marshaller m = JAXBContext.newInstance(TicketList.class).createMarshaller();
		File file = new File(path);
		file.getParentFile().mkdirs();
		FileWriter fw = new FileWriter(file);
		m.marshal(tl, fw);
		fw.close();		
	}
	
	@SuppressWarnings("unused")
	private Ticket ticket1() {
		Ticket t1 = new Ticket();
		t1.setId("Ticket1");
		t1.setReporter("jonh.smith@acme.com");
		t1.setStatus("OPEN");
		t1.setTitle("Big trouble");
		t1.setText("Something really bad has happened");
		t1.setTimestamp(System.currentTimeMillis());
		t1.addTags("TROUBLE", "REALLY_BAD");
		
		t1.addComments("poul.user@acme.com", "God save us", System.currentTimeMillis());
		return t1;
	}

	@SuppressWarnings("unused")
	private Ticket ticket2() {
		Ticket t2 = new Ticket();
		t2.setId("Ticket2");
		t2.setReporter("piter.pen@acme.com");
		t2.setStatus("CANCELED");
		t2.setTitle("SOS");
		t2.setText("Help! Help! Help!");
		t2.setTimestamp(System.currentTimeMillis());
		t2.addTags("TROUBLE", "GRAVE_DANGER");
		
		t2.addComments("bad.admin@acme.com", "Rejected due to lack of details", System.currentTimeMillis());
		return t2;
	}

	@Test
	public void loadTest() {
		// verify that file can be parsed
		loadTickets("src/test/resources/tickets/small-set.xml");
	}
	
//	@Test
//	public void writeTickets() throws JAXBException, IOException {
//		storeTickets("src/test/resources/tickets/small-set.xml", ticket1(), ticket2());
//	}
	
	@XmlRootElement(name="tickets")
	@XmlAccessorType(XmlAccessType.PROPERTY)
	public static class TicketList {

		@XmlElement(name="ticket")
		public List<Ticket> tickets = new ArrayList<Ticket>();
		
	}
	
}
