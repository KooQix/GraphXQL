package dev.kooqix.node;

import java.io.Serializable;
import java.util.UUID;

import org.apache.avro.generic.GenericRecord;

import dev.kooqix.avro.GenericData2;
import dev.kooqix.database.NodeType;

public class Node implements Serializable {
	private NodeType nodetype;
	private Long uuid;
	private GenericRecord record;

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

		this.record = new GenericData2.Record(this.nodetype.getSchema());
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

		this.record = new GenericData2.Record(this.nodetype.getSchema());
		this.record.put("uuid", this.uuid);
	}

	public void set(String key, Object value) {
		this.record.put(key, value);
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
		return this.record.toString();
	}
}
