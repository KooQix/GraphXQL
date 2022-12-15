package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class NoSuchNodeException extends Exception {
	private String nodeUUID;

	public NoSuchNodeException(long nodeUUID) {
		this.nodeUUID = "" + nodeUUID;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("NoSuchNodeException: {0}\n\nStack:\n{1}", this.nodeUUID, this.getStackTrace());
	}
}
