package dev.kooqix.relationships;

public class Relationship {
	private String value;
	private String node1;
	private String node2;

	public Relationship(String value, String node1, String node2) {
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
	public String getNode1() {
		return node1;
	}

	/**
	 * @param node1 the node1 to set
	 */
	public void setNode1(String node1) {
		this.node1 = node1;
	}

	/**
	 * @return the node2
	 */
	public String getNode2() {
		return node2;
	}

	/**
	 * @param node2 the node2 to set
	 */
	public void setNode2(String node2) {
		this.node2 = node2;
	}
}
