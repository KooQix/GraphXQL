package dev.kooqix.exceptions;

import java.text.MessageFormat;

import org.apache.spark.graphx.Edge;

public class NoSuchRelationshipException extends Exception {
	private Edge<String> relationship;

	public NoSuchRelationshipException(Edge<String> relationship) {
		this.relationship = relationship;
	}

	@Override
	public String getMessage() {
		return MessageFormat.format("NoSuchRelationshipException: {0}-{1}:{2}\n\nStack:\n{3}",
				this.relationship.srcId(), this.relationship.dstId(), this.relationship.attr(), this.getStackTrace());
	}
}
