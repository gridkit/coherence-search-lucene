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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.util.Version;

public class ReflectionAnalyzerProvider implements LuceneAnalyzerProvider, Serializable {

	private static final long serialVersionUID = 20130602L;
	
	private Class<? extends Analyzer> type;
	private Version version;
	
	public ReflectionAnalyzerProvider(Class<? extends Analyzer> type, Version version) {
		this.type = type;
		this.version = version;
	}

	@Override
	public Analyzer getAnalyzer() {
		try {
			if (version != null) {
				for(Constructor<?> c: type.getConstructors()) {
					if (c.getParameterTypes().length == 1 && c.getParameterTypes()[0] == Version.class) {
						return (Analyzer) c.newInstance(version);
					}
				}
			}		
			return type.newInstance();
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Failed to instatiate " + type.getName(), e.getCause());
		} catch (Exception e) {
			throw new RuntimeException("Failed to instatiate " + type.getName(), e);
		}
	}

	@Override
	public String toString() {
		return type.getName();
	}
}
