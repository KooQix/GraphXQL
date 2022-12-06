package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class NoSuchNodeTypeException extends Exception {
	private String nodetypeName;

	public NoSuchNodeTypeException(String nodetypeName) {
		this.nodetypeName = nodetypeName;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("NoSuchNodeTypeException: {0}\n\nStack:\n{1}", this.nodetypeName,
				this.getStackTrace());
	}
}
