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

import java.util.List;
import java.util.Map;

public interface ScoredEntries<K, V> {

	public float highScore();
	
	public int totalHits();
	
	public int size();
	
	public void fetchValues();
	
	public List<K> keys();

	public List<V> values();
	
	public List<ScoredEntry<K, V>> entries();
	
	public Map<K, V> asOrderedMap();
	
}
