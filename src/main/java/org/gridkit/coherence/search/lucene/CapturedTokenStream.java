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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeImpl;

public class CapturedTokenStream extends TokenStream {

	private List<State> tokens;
	private Iterator<State> iterator;
	private long lastPos = 0;
	
	public CapturedTokenStream(TokenStream ts) throws IOException {
		this(ts, 1, 0);
	}

	public CapturedTokenStream(TokenStream ts, int startPosition, int offset) throws IOException {
		Iterator<AttributeImpl> ai = ts.getAttributeImplsIterator();
		while(ai.hasNext()) {
			AttributeImpl attr = ai.next().clone();
			addAttributeImpl(attr);
		}
		tokens = new ArrayList<State>();
		
		append(ts, startPosition, offset);
	}

	public long getLastPosition() {
		return lastPos;
	}
	
	public void append(TokenStream ts, int positionGap, int offsetShift) throws IOException {
		PositionIncrementAttribute pi = null;
		pi = ts.getAttribute(PositionIncrementAttribute.class);
		OffsetAttribute off = null;
		if (offsetShift != 0) {
			off = ts.getAttribute(OffsetAttribute.class);
		}
		ts.reset();
		while(ts.incrementToken()) {
			if (positionGap != 0) {
				pi.setPositionIncrement(positionGap);
				positionGap = 0;
			}
			if (off != null) {
				off.setOffset(offsetShift + off.startOffset(), offsetShift + off.endOffset());
			}
			tokens.add(ts.captureState());
			lastPos += pi.getPositionIncrement();
		}
	}
	
	@Override
	public void reset() throws IOException {
		iterator = tokens.iterator();
	}
	
	@Override
	public final boolean incrementToken() throws IOException {
		// ... lucene forcing incrementToken to be final
		if (!iterator.hasNext()) {
			return false;
		}
		else {
			State s = iterator.next();
			restoreState(s);
			return true;
		}
	}	
}
