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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.junit.Ignore;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

/**
 * Test document class.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@Ignore
@Portable
@SuppressWarnings("serial")
public class Issue implements Serializable {

	@PortableProperty(1)
	public String id;

	@PortableProperty(2)
	public String project;

	@PortableProperty(3)
	public String title;

	@PortableProperty(4)
	public String description;

	@PortableProperty(5)
	public List<String> tags = Collections.emptyList();

	public String getId() {
		return id;
	}

	public String getProject() {
		return project;
	}

	public String getTitle() {
		return title;
	}

	public List<String> getTags() {
		return tags;
	}

	public String getDescription() {
		return description;
	}
	
	public String getTagLine() {
		StringBuilder sb = new StringBuilder();
		for(String tag: tags) {
			if (sb.length() > 0) {
				sb.append(' ');
			}
			sb.append(tag);
		}
		
		return sb.toString();
	}
}
