package dev.kooqix.node;

import java.io.Serializable;
import java.text.MessageFormat;

public class Field<T extends Serializable> implements Serializable {
	private String key;
	private T value;

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

	@Override
	public String toString() {
		return MessageFormat.format("{0} : {1}", key, value);
	}
}
