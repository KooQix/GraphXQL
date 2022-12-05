package dev.kooqix.exceptions;

import java.text.MessageFormat;

public class InalterableNodeException extends Exception {
	private String nodeUUID;

	public InalterableNodeException(Long nodeUUID) {
		this.nodeUUID = "" + nodeUUID;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format(
				"InalterableNodeException: node {0} \ncreate a new node to update it (Once it has been added to the database the object cannot be modified)\n\nStack:\n{1}",
				this.nodeUUID, this.getStackTrace());
	}
}
