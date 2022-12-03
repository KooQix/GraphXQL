package dev.kooqix.node;

import java.io.Serializable;
import java.util.UUID;

import java.util.HashSet;

import dev.kooqix.database.NodeType;

public class Node implements Serializable {
	private static String SEPARATOR = "--;--";

	private NodeType nodetype;
	private Long uuid;
	private HashSet<Field> fields;

	/**
	 * Load node from file
	 * 
	 * @param nodetype
	 * @param uuid
	 * @param content
	 */
	protected Node(NodeType nodetype, Long uuid) {
		this.nodetype = nodetype;
		this.uuid = uuid;

		this.fields = new HashSet<>();
	}

	/**
	 * Creates new node to add
	 * 
	 * @param nodetype
	 * @param content
	 */
	public Node(NodeType nodetype) {
		this.nodetype = nodetype;
		this.uuid = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
	}

	public <T extends Serializable> void addField(Field<T> field) {
		this.fields.add(field);
	}

	public void setFields(HashSet<Field> fields) {
		this.fields = fields;
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
	public Long getUUId() {
		return uuid;
	}

	@Override
	public String toString() {
		String content = "";

		for (Field field : fields) {
			content += SEPARATOR + (String) field.getValue();
		}

		return this.uuid + content;
	}
}
