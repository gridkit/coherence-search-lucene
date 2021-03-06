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
package org.gridkit.coherence.search.comparation.subquery;

import java.util.Random;
import java.util.TreeMap;

public class TextCorpusGenerator {

	private int wordsPerText = 64;
	private TreeMap<Integer, WordDic> wordDictionaries = new TreeMap<Integer, WordDic>();
	private int totalWeight = 0;
	
	public void setWordsPerText(int count) {
		wordsPerText = count;
	}
	
	public void addWords(int weight, String pref, int count) {
		wordDictionaries.put(totalWeight, new WordDic(pref, count));
		totalWeight += weight;
	}
	
	public String getText(long id) {
		Random rnd = new Random(id);
		int len = (int) (wordsPerText * (rnd.nextGaussian() + 1));
		if (len < 4) {
			len = 4;
		}
		StringBuilder buf = new StringBuilder();
		for(int n = 0; n != len; ++n) {
			if (buf.length() > 0) {
				buf.append(' ');
			}
			int dic = rnd.nextInt(totalWeight);
			buf.append(wordDictionaries.floorEntry(dic).getValue().next(rnd));
		}
		return buf.toString();
	}
	
	private class WordDic {
		
		private String pref;
		private int range;
		
		public WordDic(String pref, int range) {
			this.pref = pref;
			this.range = range;
		}
		
		public String next(Random rnd) {
			return pref + rnd.nextInt(range);
		}
	}
	
}
