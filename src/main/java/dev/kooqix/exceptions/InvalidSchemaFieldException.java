package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class InvalidSchemaFieldException extends Exception {
	private String message;

	public InvalidSchemaFieldException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("InvalidSchemaFieldException: {0}\n\nStack:\n{1}", this.message,
				this.getStackTrace());
	}
}
