package dev.kooqix.node;

import java.util.Map;

public class Node {
	private NodeType nodetype;
	private String uuid;
	private Map<String, Object> content;

	public Node(NodeType nodetype, String uuid, Map<String, Object> content) {
		this.nodetype = nodetype;
		this.uuid = uuid;
		this.content = content;
	}

	/**
	 * @return the content
	 */
	public Map<String, Object> getContent() {
		return content;
	}

	/**
	 * @return the nodetype
	 */
	public NodeType getNodetype() {
		return nodetype;
	}

	/**
	 * @return the uuid
	 */
	public String getUUId() {
		return uuid;
	}

}
