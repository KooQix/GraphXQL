package dev.kooqix.node;

import java.util.Map;

import dev.kooqix.exceptions.JobFailedException;

import java.util.HashMap;
import java.util.Collection;

public class NodeType {
	private String name;

	private static Map<String, NodeType> multiton = null;

	private NodeType(String name) {
		this.name = name;
	}

	public static NodeType getInstance(String name) {
		if (multiton == null)
			multiton = new HashMap<String, NodeType>();

		if (!multiton.containsKey(name))
			multiton.put(name, new NodeType(name));

		return multiton.get(name);
	}

	public static Collection<NodeType> getAll() {
		return multiton.values();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @throws JobFailedException
	 */
	public void setName(String name) throws JobFailedException {
		String oldName = this.name;

		try {
			// Set directory name
			this.name = name;
		} catch (Exception e) {
			throw new JobFailedException("Failed to rename");
		}
	}
}
