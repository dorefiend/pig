package org.apache.pig.impl.storm.state;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.pig.backend.hadoop.HDataType;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.io.NullableTuple;
import org.apache.pig.impl.io.PigNullableWritable;

import backtype.storm.tuple.Values;

import storm.trident.state.Serializer;

public class PigSerializer implements Serializer {
	
	@Override
	public byte[] serialize(Object o) {
		DataOutputBuffer dbuf = new DataOutputBuffer();
		
		try {
			if (o instanceof Values) {
				Values objl = (Values) o;
				dbuf.writeByte(0);
				// Write out the length.
				dbuf.writeInt(objl.size());
				
				// First, write the type.
				for (int i = 0; i < objl.size(); i++) {
					PigNullableWritable pnw = (PigNullableWritable) objl.get(i);
					dbuf.writeByte(HDataType.findTypeFromNullableWritable(pnw));
					pnw.write(dbuf);
				}				
			} else if (o instanceof MapWritable) {
				dbuf.writeByte(1);
				((MapWritable) o).write(dbuf);
				} else {
				
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return dbuf.getData();
	}

	@Override
	public Object deserialize(byte[] b) {
		ByteArrayInputStream bais = new ByteArrayInputStream(b);
		DataInputStream dis = new DataInputStream(bais);
		
		try {
			byte write_type = dis.readByte();
			Object ret = null;
			if (write_type == 0) {
				// Read the length.
				int values_size = dis.readInt();
				Values arr = new Values();
				
				for (int i = 0; i < values_size; i++) {
					// First read the type
					byte t = dis.readByte();
					// Get a new instance of the appropriate object.
					PigNullableWritable pnw = HDataType.getWritableComparableTypes(t).getClass().newInstance();
					pnw.readFields(dis);
					arr.add(pnw);
				}
				ret = arr;
			} else if (write_type == 1) {
				MapWritable m = new MapWritable();
				m.readFields(dis);
				ret = m;
			}
			
			return ret;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}				
	}

}
