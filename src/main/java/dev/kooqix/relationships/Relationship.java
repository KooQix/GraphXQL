package dev.kooqix.relationships;

import java.io.Serializable;

import dev.kooqix.graphxql.Node;

public class Relationship implements Serializable {
	private String value;
	private Node node1;
	private Node node2;

	public Relationship(String value, Node node1, Node node2) {
		this.value = value;
		this.node1 = node1;
		this.node2 = node2;
	}

	//////////////////// Getters and setters \\\\\\\\\\\\\\\\\\\\

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return the node1
	 */
	public Node getNode1() {
		return node1;
	}

	/**
	 * @param node1 the node1 to set
	 */
	public void setNode1(Node node1) {
		this.node1 = node1;
	}

	/**
	 * @return the node2
	 */
	public Node getNode2() {
		return node2;
	}

	/**
	 * @param node2 the node2 to set
	 */
	public void setNode2(Node node2) {
		this.node2 = node2;
	}

	@Override
	public String toString() {
		return this.node1.getUUId() + "--;--" + this.node2.getUUId() + "--;--" + this.value;
	}
}
