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

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

@XmlRootElement(name="ticket")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class Ticket implements Serializable, PortableObject {

	private static final long serialVersionUID = 20130427L;
	
	private String id;
	
	private String title;

	private String reporter;

	private String text;
	
	private String status;

	@XmlElementWrapper(name="tags")
	@XmlElement(name="tag")
	private LinkedHashSet<String> tags = new LinkedHashSet<String>();
	private long timestamp;

	@XmlElementWrapper(name="comments")
	@XmlElement(name="comment")
	private List<Comment> comments = new ArrayList<Comment>();
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getReporter() {
		return reporter;
	}

	public void setReporter(String reporter) {
		this.reporter = reporter;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Set<String> getTags() {
		return new LinkedHashSet<String>(tags);
	}

	public void addTags(String... tags) {
		this.tags.addAll(Arrays.asList(tags));
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<Comment> getComments() {
		return comments;
	}

	public void addComments(String author, String text, long timestamp) {
		this.comments.add(new Comment(author, text, timestamp));
	}
	
	public String toString() {
		try {
			StringWriter sw = new StringWriter();
			JAXBContext.newInstance(Ticket.class).createMarshaller().marshal(this, sw);
			return sw.toString();
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void readExternal(PofReader in) throws IOException {
		int id = 0;
		this.id = in.readString(++id);
		title = in.readString(++id);
		reporter = in.readString(++id);
		text = in.readString(++id);
		status = in.readString(++id);
		tags = (LinkedHashSet<String>) in.readCollection(++id, new LinkedHashSet<String>());
		timestamp = in.readLong(++id);
		comments = (List<Comment>) in.readCollection(++id, new ArrayList<Comment>());
	}

	@Override
	public void writeExternal(PofWriter out) throws IOException {
		int id = 0;
		
		out.writeString(++id, this.id);
		out.writeString(++id, title);
		out.writeString(++id, reporter);
		out.writeString(++id, text);
		out.writeString(++id, status);
		out.writeCollection(++id, tags);
		out.writeLong(++id, timestamp);
		out.writeCollection(++id, comments);
	}

	@Portable
	@XmlAccessorType(XmlAccessType.FIELD)
	public static class Comment implements Serializable {
		
		private static final long serialVersionUID = 20130427L;
		
		@PortableProperty(1)
		private String author;

		@PortableProperty(2)
		private String text;

		@PortableProperty(3)
		private long timestamp;
		
		public Comment() {
		}
		
		public Comment(String author, String text, long timestamp) {
			this.author = author;
			this.text = text;
			this.timestamp = timestamp;
		}

		public String getAuthor() {
			return author;
		}
		
		public String getText() {
			return text;
		}

		public long getTimestamp() {
			return timestamp;
		} 
	}
}
