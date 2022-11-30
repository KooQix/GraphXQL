package dev.kooqix.node;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public class Node implements Serializable {
	private NodeType nodetype;
	private Long uuid;
	// private Map<String, Object> content;
	private String content;

	// public Node(NodeType nodetype, String uuid, Map<String, Object> content) {
	// this.nodetype = nodetype;
	// this.uuid = uuid;
	// this.content = content;
	// }

	public Node(NodeType nodetype, Long uuid, String content) {
		this.nodetype = nodetype;
		this.uuid = uuid;
		this.content = content;
	}

	public Node(NodeType nodetype, String content) {
		this.nodetype = nodetype;
		this.content = content;
		this.uuid = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
	}

	/**
	 * @return the content
	 */
	// public Map<String, Object> getContent() {
	// return content;
	// }

	/**
	 * @return the nodetype
	 */
	public NodeType getNodetype() {
		return nodetype;
	}

	/**
	 * @return the uuid
	 */
	public Long getUUId() {
		return uuid;
	}

	@Override
	public String toString() {
		return this.uuid + "--;--" + this.content;
	}
}
