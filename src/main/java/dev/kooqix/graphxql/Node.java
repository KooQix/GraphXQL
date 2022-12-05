package dev.kooqix.graphxql;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import dev.kooqix.exceptions.InalterableNodeException;

public class Node implements Serializable, Comparable<Node> {
	private static String SEPARATOR = "--;--";

	private NodeType nodetype;
	private Long uuid;
	private HashSet<Field> fields;

	/**
	 * Creates new node to add
	 * While the node has not been added to the database (db.addNode(node)), its
	 * uuid is null
	 * 
	 * @param nodetype
	 * @param content
	 */
	public Node(NodeType nodetype) {
		this.nodetype = nodetype;
		this.fields = new HashSet<>();
		this.uuid = null;
	}

	public <T extends Serializable> void addField(Field<T> field)
			throws IllegalArgumentException, IOException, InalterableNodeException {
		if (this.uuid == null)
			this.fields.add(field);
		else
			throw new InalterableNodeException(this.uuid);
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
	 * Called when the node is added to the database
	 * 
	 * @param db
	 * @param uuid
	 */
	protected void init(long uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return a copy of the fields
	 */
	public HashSet<Field> getFields() {
		HashSet<Field> resFields = new HashSet<Field>();
		Iterator<Field> it = this.fields.iterator();
		Field field;
		while (it.hasNext()) {
			field = it.next();
			resFields.add(new Field(field.getKey(), field.getValue()));
		}
		return resFields;
	}

	/**
	 * Can set fields solely if
	 * 
	 * @param fields
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws InalterableNodeException
	 */
	public void setFields(HashSet<Field> fields)
			throws IllegalArgumentException, IOException, InalterableNodeException {
		if (this.uuid == null)
			this.fields = fields;
		else
			throw new InalterableNodeException(this.uuid);

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
	public Long getUUID() {
		return this.uuid;
	}

	@Override
	public String toString() {
		String content = "";

		for (Field field : this.fields) {
			content += SEPARATOR + field.toString();
		}

		return this.uuid + content;
	}

	@Override
	public int compareTo(Node o) {
		return Long.compare(this.getUUID(), o.getUUID());
	}

	public static int compare(Node node1, Node node2) {
		return node1.compareTo(node2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodetype == null) ? 0 : nodetype.hashCode());
		result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (nodetype == null) {
			if (other.nodetype != null)
				return false;
		} else if (!nodetype.equals(other.nodetype))
			return false;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

}
