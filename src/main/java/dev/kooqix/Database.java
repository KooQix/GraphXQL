package dev.kooqix;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.graphx.EdgeRDD;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.PartitionStrategy;
import org.apache.spark.graphx.TripletFields;
import org.apache.spark.graphx.VertexRDD;
import org.apache.spark.rdd.RDD;
import org.apache.spark.storage.StorageLevel;

import dev.kooqix.exceptions.JobFailedException;
import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.node.Node;
import dev.kooqix.node.NodeType;
import dev.kooqix.relationships.Relationship;

public class Database {

	//////////////////// Configuration variables \\\\\\\\\\\\\\\\\\\\

	private static final String GRAPHXQL_HOME_TMP = System.getenv("GRAPHXQL_HOME");

	private static final String GRAPHXQL_HOME = GRAPHXQL_HOME_TMP.substring(GRAPHXQL_HOME_TMP.length() - 1).equals("/")
			? GRAPHXQL_HOME_TMP.substring(0, GRAPHXQL_HOME_TMP.length() - 1)
			: GRAPHXQL_HOME_TMP;

	private static final String RELATIONSHIPS_FILE = "relationships.parquet";
	private static final String NODETYPES_DIRECTORY_NAME = "nodetypes";
	private static final String DIR_DATABASES = MessageFormat.format("hdfs://{0}/databases",
			GRAPHXQL_HOME);

	//////////////////// Attributes \\\\\\\\\\\\\\\\\\\\

	private String dir;
	private String name;
	private Collection<NodeType> nodetypes;
	private Graph<Node, Relationship> graph;

	private static Map<String, Database> db = new HashMap<String, Database>();

	private Database(String name) {
		this.setName(name);

		// Load available nodetypes
		NodeType.load(MessageFormat.format("{0}/{1}", this.dir, NODETYPES_DIRECTORY_NAME));
		this.nodetypes = NodeType.getAll();
	}

	/**
	 * Get the database instance of name name
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchDatabaseException
	 */
	public Database getInstance(String name) throws NoSuchDatabaseException {
		if (!db.containsKey(name)) {
			// Db exists, add to map
			if (getAll().contains(name))
				db.put(name, new Database(name));
			else
				throw new NoSuchDatabaseException(name);
		}

		return db.get(name);
	}

	/**
	 * Create new database
	 * 
	 * @param name
	 */
	public static void create(String name) {

		// Create db directory
		new File(MessageFormat.format("{0}/{1}", DIR_DATABASES, name.toLowerCase())).mkdirs();

		// Create files
		String dir = MessageFormat.format("{0}/{1}", DIR_DATABASES, name.toLowerCase());

		new File(MessageFormat.format("{0}/{1}", dir, RELATIONSHIPS_FILE));
		new File(MessageFormat.format("{0}/{1}", dir, NODETYPES_DIRECTORY_NAME)).mkdirs();

		// Add to map
		db.put(name, new Database(name));
	}

	/**
	 * Update database
	 * 
	 * @param name
	 * @throws JobFailedException
	 */
	public void update(String name) throws JobFailedException {
		File dbDir = new File(this.dir);

		if (dbDir.renameTo(new File(MessageFormat.format("{0}/{1}", DIR_DATABASES, this.name))))
			this.setName(name);
		else
			throw new JobFailedException(MessageFormat.format("Unable to update {0} database", this.name));

	}

	public void delete() {
		// Delete all directory
	}

	/**
	 * Set the name and directory of the database (based on its name)
	 * 
	 * @param name The name of the database
	 */
	private void setName(String name) {
		this.name = name.toLowerCase();
		this.dir = MessageFormat.format("{0}/{1}", DIR_DATABASES, this.name);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get all the databases
	 * 
	 * @return
	 */
	public static List<String> getAll() {
		File[] directories = new File(DIR_DATABASES).listFiles(File::isDirectory);

		List<String> databases = new ArrayList<String>();
		String[] path;
		for (File file : directories) {
			path = file.getPath().split("/");
			databases.add(path[path.length - 1]);
		}
		return databases;
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}\n\t{1} nodetypes", this.name.toUpperCase(), this.nodetypes.size());
	}
}
