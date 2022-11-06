package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class JobFailedException extends Exception {
	private String message;

	public JobFailedException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("JobFailedException: {0}\n\nStack:\n{1}", this.message, this.getStackTrace());
	}
}
