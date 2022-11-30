package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class NodeTypeExistsException extends Exception {
	private String message;

	public NodeTypeExistsException(String message) {
		this.message = message;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("NodeTypeExistsException: {0}\n\nStack:\n{1}", this.message, this.getStackTrace());
	}
}
