package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class DatabaseExistsException extends Exception {
	private String databaseName;

	public DatabaseExistsException(String databaseName) {
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("DatabaseExistsException: {0}\n\nStack:\n{1}", this.databaseName,
				this.getStackTrace());
	}
}
