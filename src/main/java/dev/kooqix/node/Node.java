package dev.kooqix.node;

import java.util.List;

public class Node {
	private NodeType nodetype;

	public <T> Node(NodeType nodetype, List<Field<T>> fields) {
		this.nodetype = nodetype;

	}

}
