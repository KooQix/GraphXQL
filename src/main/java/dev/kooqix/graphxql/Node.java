package dev.kooqix.graphxql;

import java.io.Serializable;
import java.util.UUID;
import java.util.HashSet;
import java.util.Iterator;

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

		this.fields = new HashSet<>();
	}

	public <T extends Serializable> void addField(Field<T> field) {
		this.fields.add(field);
	}

	public Serializable getFieldValue(String key) throws NoSuchFieldException {
		Iterator<Field> it = this.fields.iterator();
		Field field;
		while (it.hasNext()) {
			field = it.next();
			if (field.getKey().equals(key))
				return field.getValue();
		}
		throw new NoSuchFieldException(key);
	}

	/**
	 * @return the fields
	 */
	public HashSet<Field> getFields() {
		return fields;
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
