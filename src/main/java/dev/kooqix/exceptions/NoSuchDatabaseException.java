package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class NoSuchDatabaseException extends Exception {
	private String name;

	public NoSuchDatabaseException(String name) {
		this.name = name;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("NoSuchDatabase: {0}\n\nStack:\n{1}", this.name, this.getStackTrace());
	}
}
