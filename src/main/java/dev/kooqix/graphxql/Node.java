package dev.kooqix.graphxql;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Node implements Serializable, Comparable<Node> {
	private static String SEPARATOR = "--;--";

	private NodeType nodetype;
	private Long uuid;
	private Set<Field> fields; // Fields are key value pairs

	/**
	 * Creates new node to add
	 * While the node has not been added to the database (db.addNode(node)), its
	 * uuid is null
	 * 
	 * @param nodetype
	 */
	public Node(NodeType nodetype) {
		this.nodetype = nodetype;
		this.fields = new HashSet<>();

		// Not linked to database => uuid is null
		this.uuid = null;
	}

	/**
	 * Called by database when loading the node from disk
	 * 
	 * @param nodetype
	 * @param uuid
	 */
	protected Node(NodeType nodetype, Long uuid) {
		this.nodetype = nodetype;
		this.fields = new HashSet<>();
		this.uuid = uuid;
	}

	/**
	 * Copy
	 * 
	 * @param nodetype
	 * @param fields
	 */
	private Node(NodeType nodetype, Set<Field> fields) {
		this.nodetype = nodetype;
		this.fields = fields;
		this.uuid = null;
	}

	/**
	 * Add a field (if field already exist, the key is present, pass)
	 * 
	 * @param <T>
	 * @param field
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public <T extends Serializable> void addField(Field<T> field)
			throws IllegalArgumentException {
		this.fields.add(field);
	}

	/**
	 * Set a field value (replace if key already exist)
	 * 
	 * @param field
	 */
	public void setField(Field field) {
		if (this.fields.contains(field))
			this.fields.remove(field);

		this.fields.add(field);
	}

	/**
	 * Set fields (replace)
	 * 
	 * @param fields
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void setFields(Set<Field> fields)
			throws IllegalArgumentException {
		this.fields = fields;
	}

	/**
	 * Get the value of a field
	 * 
	 * @param key
	 * @return
	 * @throws NoSuchFieldException If field does not exist
	 */
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
	 * @return fields
	 */
	public Set<Field> getFields() {
		return this.fields;
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

	/**
	 * Create a copy of a node (new node without uuid)
	 * 
	 * @return
	 */
	public Node copy() {
		return new Node(this.nodetype, this.fields);
	}

	/**
	 * Called by the database object when the node is added
	 * 
	 * @param uuid
	 */
	protected void init(long uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		String content = "";

		for (Field field : this.fields) {
			content += SEPARATOR + field.toString();
		}

		return this.uuid + content;
	}

	/**
	 * Nodes are compared by uuid
	 */
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

	/**
	 * 2 nodes are equals if they have the same nodetype and uuid
	 */
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
