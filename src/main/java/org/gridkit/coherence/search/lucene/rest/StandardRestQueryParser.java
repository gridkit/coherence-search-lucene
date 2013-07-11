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
package org.gridkit.coherence.search.lucene.rest;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.gridkit.coherence.search.lucene.xml.NaiveXmlSchema;

public class StandardRestQueryParser implements RestQueryParser {
	
	private static final Pattern VAR = Pattern.compile("[$][{]([\\w\\d]+)[}]");
	
	private StandardQueryParser parser;
	
	public StandardRestQueryParser() {
		parser = new StandardQueryParser(new StandardAnalyzer(Version.LUCENE_42));
	}
	
	@Override
	public Query parse(String line, Map<String, Object> parameters) {
		StringBuilder sb = new StringBuilder();
		transform(sb, line, parameters);
		String query = sb.toString();
		try {
			return parser.parse(sb.toString(), NaiveXmlSchema.WHOLE_TEXT);
		}
		catch (QueryNodeException e) {
			throw new IllegalArgumentException("Cannot parse query [" + query + "]", e);
		}
	}

	private void transform(StringBuilder sb, String line, Map<String, Object> parameters) {
		if (sb.length() > 4 << 10) {
			throw new IllegalArgumentException("Search query is too long");
		}
		Matcher m = VAR.matcher(line);
		if (m.find()) {
			String var = m.group(1);
			Object subq = parameters.get(var);
			if (subq == null) {
				throw new IllegalArgumentException("Query parameter '" + var + "' is missing");
			}
			sb.append(line.substring(0, m.start()));			
			sb.append("(");
			transform(sb, String.valueOf(subq), parameters);
			sb.append(")");
			transform(sb, line.substring(m.end()), parameters);
		}
		else {
			sb.append(line);
		}
		return;
	}
}
