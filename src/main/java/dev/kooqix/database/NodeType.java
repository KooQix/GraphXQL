package dev.kooqix.database;

import dev.kooqix.exceptions.InvalidSchemaFieldException;
import dev.kooqix.exceptions.JobFailedException;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class NodeType implements Serializable {

	private String name;
	private String filename;

	private String path;

	/**
	 * When loading
	 * 
	 * @param name
	 */
	protected NodeType(String name) {
		this.name = name.toLowerCase();
		this.filename = this.name + ".avro";
	}

	/**
	 * Create new nodetype
	 * 
	 * @param name
	 * @param schemaFile
	 * @throws IOException
	 * @throws InvalidSchemaFieldException
	 */
	public NodeType(String name, String schemaFile) throws IOException, InvalidSchemaFieldException {
		this.name = name.toLowerCase();

		this.filename = this.name + ".avro";
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

	/**
	 * @return the filename
	 */
	protected String getFilename() {
		return filename;
	}

	protected void setPath(String path) {
		this.path = path + "/" + this.filename;
	}
}
