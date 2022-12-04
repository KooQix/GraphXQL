package dev.kooqix.graphxql;

import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.kooqix.exceptions.NodeTypeExistsException;

public class NodeTypes implements Serializable {
	private Database database;

	private String directory;

	private Map<String, NodeType> multiton = null;

	protected NodeTypes(Database database, String directory) throws IOException {
		this.database = database;
		this.directory = directory;

		multiton = new HashMap<String, NodeType>();

		List<String> nodetypes = Hdfs.listDirectories(this.directory);

		for (String nodetypeFile : nodetypes) {
			NodeType nodetype = new NodeType(nodetypeFile);
			nodetype.setPath(this.database.getDirNodetypes());
			multiton.put(nodetypeFile, nodetype);

		}
	}

	public NodeType getInstance(String name) {
		return multiton.getOrDefault(name, null);
	}

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
		multiton.put(nodetype.getName(), nodetype);
		nodetype.setPath(this.database.getDirNodetypes());
		// try {
		// Hdfs.createDirectory(MessageFormat.format("{0}/{1}", directory,
		// nodetype.getName()), false);

		// multiton.put(nodetype.getName(), nodetype);
		// nodetype.setPath(this.database.getDirNodetypes());
		// } catch (NodeTypeExistsException e) {
		// multiton.put(nodetype.getName(), nodetype);
		// nodetype.setPath(this.database.getDirNodetypes());
		// } catch (Exception e) {

		// }
	}
}
