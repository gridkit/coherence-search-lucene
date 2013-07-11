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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Lucene 4.0 has dropped support for {@link Serializable} forcing
 * introduction of this ugly workaround.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class Envelop<T> implements Serializable {

	private static final long serialVersionUID = 20130601L;

	private transient T payload;

	public Envelop(T payload) {
		this.payload = payload;
	}

	public T getPayload() {
		return payload;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		Kryo kryo = KryoHelper.getKryo();
		Output ko = new Output(out);
		kryo.writeClassAndObject(ko, payload);
		ko.flush();
	}

	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		Kryo kryo = KryoHelper.getKryo();
		Input ki = new Input(in);
		payload = (T) kryo.readClassAndObject(ki);
		new String();
	}
}
