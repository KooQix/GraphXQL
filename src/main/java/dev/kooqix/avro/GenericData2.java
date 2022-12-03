package dev.kooqix.avro;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

public class GenericData2 extends GenericData implements Serializable {

	private static final GenericData2 INSTANCE = new GenericData2();

	/** Return the singleton instance. */
	public static GenericData2 get() {
		return INSTANCE;
	}

	/**
	 * Comparison implementation. When equals is true, only checks for equality, not
	 * for order.
	 */
	@SuppressWarnings(value = "unchecked")
	protected int compare(Object o1, Object o2, Schema s, boolean equals) {
		if (o1 == o2)
			return 0;
		switch (s.getType()) {
			case RECORD:
				for (Field f : s.getFields()) {
					if (f.order() == Field.Order.IGNORE)
						continue; // ignore this field
					int pos = f.pos();
					String name = f.name();
					int compare = compare(getField(o1, name, pos), getField(o2, name, pos), f.schema(), equals);
					if (compare != 0) // not equal
						return f.order() == Field.Order.DESCENDING ? -compare : compare;
				}
				return 0;
			case ENUM:
				return s.getEnumOrdinal(o1.toString()) - s.getEnumOrdinal(o2.toString());
			case ARRAY:
				Collection a1 = (Collection) o1;
				Collection a2 = (Collection) o2;
				Iterator e1 = a1.iterator();
				Iterator e2 = a2.iterator();
				Schema elementType = s.getElementType();
				while (e1.hasNext() && e2.hasNext()) {
					int compare = compare(e1.next(), e2.next(), elementType, equals);
					if (compare != 0)
						return compare;
				}
				return e1.hasNext() ? 1 : (e2.hasNext() ? -1 : 0);
			case MAP:
				if (equals)
					return o1.equals(o2) ? 0 : 1;
				throw new AvroRuntimeException("Can't compare maps!");
			case UNION:
				int i1 = resolveUnion(s, o1);
				int i2 = resolveUnion(s, o2);
				return (i1 == i2) ? compare(o1, o2, s.getTypes().get(i1), equals) : Integer.compare(i1, i2);
			case NULL:
				return 0;
			case STRING:
				Utf8 u1 = o1 instanceof Utf8 ? (Utf8) o1 : new Utf8(o1.toString());
				Utf8 u2 = o2 instanceof Utf8 ? (Utf8) o2 : new Utf8(o2.toString());
				return u1.compareTo(u2);
			default:
				return ((Comparable) o1).compareTo(o2);
		}
	}

	/**
	 * Default implementation of {@link GenericRecord}. Note that this
	 * implementation does not fill in default values for fields if they are not
	 * specified; use {@link GenericRecordBuilder} in that case.
	 *
	 * @see GenericRecordBuilder
	 */
	public static class Record implements GenericRecord, Comparable<Record>, Serializable {
		private final Schema schema;
		private final Object[] values;

		public Record(Schema schema) {
			if (schema == null || !Type.RECORD.equals(schema.getType()))
				throw new AvroRuntimeException("Not a record schema: " + schema);
			this.schema = schema;
			this.values = new Object[schema.getFields().size()];
		}

		public Record(Record other, boolean deepCopy) {
			schema = other.schema;
			values = new Object[schema.getFields().size()];
			if (deepCopy) {
				for (int ii = 0; ii < values.length; ii++) {
					values[ii] = INSTANCE.deepCopy(schema.getFields().get(ii).schema(), other.values[ii]);
				}
			} else {
				System.arraycopy(other.values, 0, values, 0, other.values.length);
			}
		}

		@Override
		public Schema getSchema() {
			return schema;
		}

		@Override
		public void put(String key, Object value) {
			Schema.Field field = schema.getField(key);
			if (field == null) {
				throw new AvroRuntimeException("Not a valid schema field: " + key);
			}

			values[field.pos()] = value;
		}

		@Override
		public void put(int i, Object v) {
			values[i] = v;
		}

		@Override
		public Object get(String key) {
			Field field = schema.getField(key);
			if (field == null) {
				throw new AvroRuntimeException("Not a valid schema field: " + key);
			}
			return values[field.pos()];
		}

		@Override
		public Object get(int i) {
			return values[i];
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true; // identical object
			if (!(o instanceof Record))
				return false; // not a record
			Record that = (Record) o;
			if (!this.schema.equals(that.schema))
				return false; // not the same schema
			return GenericData2.get().compare(this, that, schema, true) == 0;
		}

		@Override
		public int hashCode() {
			return GenericData.get().hashCode(this, schema);
		}

		@Override
		public int compareTo(Record that) {
			return GenericData.get().compare(this, that, schema);
		}

		@Override
		public String toString() {
			return GenericData.get().toString(this);
		}
	}
}
