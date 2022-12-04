package dev.kooqix.graphxql;

import dev.kooqix.exceptions.JobFailedException;

import java.io.Serializable;

public class NodeType implements Serializable {

	private String name;
	private String path;

	public NodeType(String name) {
		this.name = name.toLowerCase();
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
	 * @return the path
	 */
	protected String getPath() {
		return path;
	}

	protected void setPath(String path) {
		this.path = path + "/" + this.name;
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
