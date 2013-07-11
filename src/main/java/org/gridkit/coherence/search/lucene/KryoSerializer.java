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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.Serializer;
import com.tangosol.io.WriteBuffer.BufferOutput;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

/**
 * Lucene 4.0 has removed serializeable interface from queiry class.
 * Kryo library is used to provide generic alternative.
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class KryoSerializer implements Serializer, PofSerializer {

	public static Object fromBytes(byte[] buf) throws IOException {
		Kryo kryo = KryoHelper.getKryo();
		Input in = new Input(buf);
		return kryo.readClassAndObject(in);
	}

	public static byte[] toBytes(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Kryo kryo = KryoHelper.getKryo();
		Output out = new Output(bos);
		kryo.writeClassAndObject(out, object);
		out.close();
		return bos.toByteArray();
	}

	@Override
	public Object deserialize(BufferInput in) throws IOException {
		byte[] buf = new byte[in.available()];
		in.read(buf);
		return fromBytes(buf);
	}

	@Override
	public Object deserialize(PofReader in) throws IOException {
		byte[] data = in.readByteArray(0);
		in.readRemainder();
		return fromBytes(data);
	}
	
	@Override
	public void serialize(BufferOutput out, Object object) throws IOException {
		byte[] byteArray = toBytes(object);
		out.write(byteArray);
	}

	@Override
	public void serialize(PofWriter out, Object object)	throws IOException {
		byte[] data = toBytes(object);
		out.writeByteArray(0, data);
		out.writeRemainder(null);
	}
}
