package dev.kooqix.graphxql;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.kooqix.exceptions.NodeTypeExistsException;

/**
 * Nodetypes linked to a given database (each is a singleton)
 */
public class NodeTypes implements Serializable {
	private Database database;

	private String directory; // Directory of the nodetypes for the given db

	private Map<String, NodeType> multiton = null; // Stores each database NodeType as singleton

	/**
	 * Called by the database when instantiated
	 * 
	 * @param database
	 * @param directory
	 * @throws IOException
	 */
	protected NodeTypes(Database database, String directory) throws IOException {
		this.database = database;
		this.directory = directory;

		multiton = new HashMap<String, NodeType>();

		// Get all available nodetype of the database and load them
		List<String> nodetypes = Hdfs.listDirectories(this.directory);

		for (String nodetypeFile : nodetypes) {
			NodeType nodetype = new NodeType(nodetypeFile);
			nodetype.setPath(this.database.getDirNodetypes());
			multiton.put(nodetypeFile, nodetype);
		}
	}

	/**
	 * Return a nodetype instance
	 * 
	 * @param name
	 * @return
	 */
	public NodeType getInstance(String name) {
		return multiton.getOrDefault(name, null);
	}

	/**
	 * Get all nodetypes linked to database
	 * 
	 * @return
	 */
	public Collection<NodeType> getAll() {
		return multiton.values();
	}

	/**
	 * Create new nodetype: Create directory and add to multiton available nodetypes
	 * for the linked database
	 * 
	 * @param nodetype
	 */
	public void addNodeType(NodeType nodetype) {
		try {
			Hdfs.createDirectory(MessageFormat.format("{0}/{1}", directory,
					nodetype.getName()), false);

			multiton.put(nodetype.getName(), nodetype);
			nodetype.setPath(this.database.getDirNodetypes());
		} catch (NodeTypeExistsException e) {
			multiton.put(nodetype.getName(), nodetype);
			nodetype.setPath(this.database.getDirNodetypes());
		} catch (Exception e) {
		}
	}
}
