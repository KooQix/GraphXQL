package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class NoSuchFieldException extends Exception {
	private String message;

	public NoSuchFieldException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("NoSuchFieldException: {0}\n\nStack:\n{1}", this.message, this.getStackTrace());
	}
}
