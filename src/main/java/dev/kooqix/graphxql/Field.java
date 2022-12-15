package dev.kooqix.graphxql;

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Key value pair
 */
public class Field<T extends Serializable> implements Serializable {
	private String key;
	private T value;

	private static final String SEPARATOR = " : ";

	public Field(String key, T value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	protected static String getSeparator() {
		return SEPARATOR;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}{1}{2}", key, SEPARATOR, value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}

	/**
	 * 2 fields are equals if they have the same key
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Field other = (Field) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}
}
