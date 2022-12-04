package dev.kooqix.graphxql;

import java.io.Serializable;
import java.text.MessageFormat;

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
}
