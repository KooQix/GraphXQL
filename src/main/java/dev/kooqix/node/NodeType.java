package dev.kooqix.node;

import java.util.Map;
import java.util.UUID;

import dev.kooqix.exceptions.JobFailedException;

import java.util.HashMap;
import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;

public class NodeType implements Serializable {
	private String name;

	public NodeType(String name) {
		this.name = name;
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

		// // Set directory name
		// File nodetypeFile = new File(MessageFormat.format("{0}/{1}.{2}", directory,
		// oldName, EXTENSION));

		// if (nodetypeFile.renameTo(new File(MessageFormat.format("{0}/{1}.{2}",
		// directory, name, EXTENSION))))
		// this.name = name;
		// else
		// throw new JobFailedException("Failed to rename");
	}

	public String getUUID() {
		String uuid = UUID.randomUUID().toString();

		// Check for all uuids for this nodetype (avro file)
		boolean exists = false;

		while (exists)
			uuid = UUID.randomUUID().toString();

		return this.name + "-" + uuid;
	}

}
