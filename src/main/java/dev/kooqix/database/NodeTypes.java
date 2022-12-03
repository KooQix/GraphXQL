package dev.kooqix.database;

import com.esotericsoftware.minlog.Log;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.kooqix.exceptions.NodeTypeExistsException;

public class NodeTypes {
	private Database database;

	private String directory;

	private Map<String, NodeType> multiton = null;

	protected NodeTypes(Database database, String directory) throws IOException {
		this.database = database;
		this.directory = directory;

		multiton = new HashMap<String, NodeType>();

		List<String> nodetypes = Hdfs.listFiles(this.directory);

		String name;

		for (String nodetypeFile : nodetypes) {
			name = nodetypeFile.replaceFirst(".avro", "");
			NodeType nodetype = new NodeType(name);
			nodetype.setPath(this.database.getDirNodetypes());
			multiton.put(name, nodetype);

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
		try {
			Hdfs.createDirectory(MessageFormat.format("{0}/{1}", directory, nodetype.getName()), false);

			nodetype.setPath(this.database.getDirNodetypes());
			multiton.put(nodetype.getName(), nodetype);
		} catch (NodeTypeExistsException e) {
			multiton.put(nodetype.getName(), nodetype);
			nodetype.setPath(this.database.getDirNodetypes());
		} catch (Exception e) {

		}
	}
}
