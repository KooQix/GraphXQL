package dev.kooqix.database;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.kooqix.node.NodeType;

public class NodeTypes {
	private Database database;

	private String directory;

	private static String EXTENSION = "avro";

	private Map<String, NodeType> multiton = null;

	protected NodeTypes(Database database, String directory) throws IOException {
		this.database = database;
		this.directory = directory;

		multiton = new HashMap<String, NodeType>();

		List<String> nodetypes = Hdfs.listFiles(this.directory);

		String name;

		for (String nodetypeFile : nodetypes) {
			name = nodetypeFile.split(".")[0];
			multiton.put(name, new NodeType(name));
		}
	}

	public NodeType getInstance(String name) {
		return multiton.getOrDefault(name, null);
	}

	public Collection<NodeType> getAll() {
		return multiton.values();
	}
}