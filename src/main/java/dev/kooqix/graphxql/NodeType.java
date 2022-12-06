package dev.kooqix.graphxql;

import dev.kooqix.exceptions.JobFailedException;

import java.io.Serializable;

/**
 * Each node has a nodetype defining what kind of node it is
 */
public class NodeType implements Serializable {

	private String name; // Name of the nodetype
	private String path; // Where nodes of this type are stored
	private String dir;

	/**
	 * Create a new nodetype (not linked to database)
	 * 
	 * @param name
	 */
	public NodeType(String name) {
		this.name = name.toLowerCase();
		this.path = null;
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
		String newName = name.toLowerCase();

		// Nodetype is linked to database
		if (this.path != null) {
			try {
				Hdfs.renameTo(this.path, this.dir + "/" + newName);
			} catch (Exception e) {
				throw new JobFailedException("Failed to rename " + this.name + " to " + newName);
			}
		}
		this.name = newName;
	}

	/**
	 * @return the path
	 */
	protected String getPath() {
		return path;
	}

	/**
	 * Path to disk (depends on the database it is linked to)
	 * 
	 * @param path
	 */
	protected void setPath(String path) {
		this.path = path + "/" + this.name;
		this.dir = path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		NodeType other = (NodeType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

}
