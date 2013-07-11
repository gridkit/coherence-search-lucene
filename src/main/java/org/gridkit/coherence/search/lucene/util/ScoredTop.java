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
package org.gridkit.coherence.search.lucene.util;

import java.io.IOException;
import java.io.Serializable;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.gridkit.coherence.search.lucene.PartitionedIndexProcessor.IndexAggregationContext;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

public class ScoredTop implements PortableObject, Serializable {
	
	private static final long serialVersionUID = 20110823L;

	Object[] keys;
	float[] scores;
	Object[] sortKey;
	
	private int totalHits;
	private float maxScore;
	
	// PortableObject no-arg constructor
	public ScoredTop() {};
	
	public ScoredTop(TopDocs topDocs, IndexAggregationContext context) {
		totalHits = topDocs.totalHits;
		maxScore = topDocs.getMaxScore();
		keys = new Object[topDocs.scoreDocs.length];
		scores = new float[topDocs.scoreDocs.length];
		for (int i = 0; i != topDocs.scoreDocs.length; ++i) {
			ScoreDoc sd = topDocs.scoreDocs[i];
			keys[i] = context.docIdToKey(sd.doc);
			scores[i] = sd.doc;
		}		
	}
	
	public int getTotalHits() {
		return totalHits;
	}

	public void setTotalHits(int totalHits) {
		this.totalHits = totalHits;
	}

	public float getMaxScore() {
		return maxScore;
	}

	public void setMaxScore(float maxScore) {
		this.maxScore = maxScore;
	}

	public int size() {
		return keys.length;
	}
	
	@Override
	public void readExternal(PofReader reader) throws IOException {
		int prop = 1;
		keys = reader.readObjectArray(prop++, new Object[0]);
		scores = reader.readFloatArray(prop++);
		totalHits = reader.readInt(prop++);
		maxScore = reader.readFloat(prop++);		
	}
	
	@Override
	public void writeExternal(PofWriter writer) throws IOException {
		int prop = 1;
		writer.writeObjectArray(prop++, keys);
		writer.writeFloatArray(prop++, scores);
		writer.writeInt(prop++, totalHits);
		writer.writeFloat(prop++, maxScore);
	}
}
