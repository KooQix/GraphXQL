package dev.kooqix.graphxql;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.Graph;
import org.apache.spark.storage.StorageLevel;
import org.apache.spark.util.LongAccumulator;

import dev.kooqix.exceptions.DatabaseExistsException;
import dev.kooqix.exceptions.JobFailedException;
import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.exceptions.NoSuchNodeException;
import dev.kooqix.exceptions.NoSuchNodeTypeException;
import dev.kooqix.exceptions.NoSuchRelationshipException;
import scala.Option;
import scala.Tuple2;
import scala.reflect.ClassTag;

public class Database implements Serializable {

	private static final String APP_NAME = "GraphXQL";
	private static String SEPARATOR = "--;--";

	//////////////////// Configuration variables \\\\\\\\\\\\\\\\\\\\

	private static String GRAPHXQL_HOME; // environment variable

	private static final String RELATIONSHIPS_DIRECTORY_NAME = "relationships";
	private static final String NODETYPES_DIRECTORY_NAME = "nodetypes";

	private static String DIR_DATABASES; // databases directory

	//////////////////// Spark context \\\\\\\\\\\\\\\\\\\\

	private static final SparkConf conf = new SparkConf().setAppName(APP_NAME);
	private static JavaSparkContext sc;

	private static boolean confInit = false;

	//////////////////// Attributes \\\\\\\\\\\\\\\\\\\\

	private String dir; // Directory of this database
	private String dirNodetypes; // Directory of the nodetypes for this database
	private String dirRelationships; // Directory of the relationships for this database
	private String name; // Name of the database
	private NodeTypes nodetypes; // Nodetypes associated with this database
	private Graph<Node, String> graph;

	private LongAccumulator acc; // To generate incremental UUID

	private static ClassTag<Node> vertexTag = scala.reflect.ClassTag$.MODULE$.apply(Node.class);
	private static ClassTag<String> edgesTag = scala.reflect.ClassTag$.MODULE$.apply(String.class);

	// Open databases (multiton, 1 singleton per database to ensure consistency)
	private static Map<String, Database> db = new HashMap<>();

	/**
	 * Get Spark configuration variables and init databases directory
	 */
	private static void initConf() {
		if (!confInit) {
			sc = new JavaSparkContext(conf);
			GRAPHXQL_HOME = conf.get("spark.yarn.appMasterEnv.GRAPHXQL_HOME");
			DIR_DATABASES = MessageFormat.format("{0}/databases",
					GRAPHXQL_HOME);

			confInit = true;
		}
	}

	/**
	 * Load database
	 * 
	 * @param name
	 * @throws IOException
	 */
	private Database(String name) throws IOException {
		initConf();
		this.acc = new LongAccumulator();
		this.acc.register(sc.sc(), Option.apply("IDAcc"), false);

		this.setName(name);

		// Load available nodetypes, for the given database
		this.nodetypes = new NodeTypes(
				this,
				this.dirNodetypes);

		//////////////////// Load graph \\\\\\\\\\\\\\\\\\\\

		JavaRDD<Tuple2<Object, Node>> verticesRDD;
		JavaRDD<Edge<String>> edgesRDD;

		if (!this.nodetypes.getAll().isEmpty()) {
			// Load vertices for all nodetypes.... for now, the first
			NodeType nodetype;
			Iterator<NodeType> it = this.nodetypes.getAll().iterator();
			nodetype = it.next();

			verticesRDD = this.loadVertices(nodetype, null);
			edgesRDD = this.loadEdges(null);

			while (it.hasNext()) {
				nodetype = it.next();
				verticesRDD.union(this.loadVertices(nodetype, null));
				edgesRDD.union(this.loadEdges(null));
			}

			// Init accumulator with the maximum node's ID
			this.initAcc();

		} else {
			// No nodetypes yet, empty rdds
			verticesRDD = sc.emptyRDD();
			edgesRDD = sc.emptyRDD();
		}

		// Load graph
		this.setGraph(verticesRDD, edgesRDD);
	}

	/**
	 * Load vertices from file
	 * 
	 * @param nodetype
	 * @param filename Load solely one file from the specified nodetype if not null,
	 *                 else all
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private JavaRDD<Tuple2<Object, Node>> loadVertices(NodeType nodetype, String filename)
			throws IllegalArgumentException, IOException {

		String dir;
		if (filename == null) {
			dir = MessageFormat.format("{0}/{1}", this.dirNodetypes,
					nodetype.getName());
		} else {
			dir = MessageFormat.format("{0}/{1}/{2}", this.dirNodetypes,
					nodetype.getName(), filename);
		}

		if (!Hdfs.fileExists(dir))
			return sc.emptyRDD();

		return sc.textFile(dir).map(
				new Function<String, Tuple2<Object, Node>>() {
					public Tuple2<Object, Node> call(String line) throws Exception {
						// uuid, fields
						String[] attributes = line.split(SEPARATOR);
						int i;
						String[] field;

						// Create node object and add its fields
						Node node = new Node(nodetype, Long.parseLong(attributes[0]));

						for (i = 1; i < attributes.length; i++) {
							field = attributes[i].split(Field.getSeparator());
							try {
								node.addField(new Field(field[0], field[1]));
							} catch (Exception e) {
							}
						}

						return new Tuple2<>(node.getUUID(), node);
					}
				});
	}

	/**
	 * Load edges from file
	 * 
	 * @param filename Load only one file if filename != null, else load all under
	 *                 the relationship directory
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private JavaRDD<Edge<String>> loadEdges(String filename)
			throws IllegalArgumentException, IOException {

		String dir;
		if (filename == null) {
			dir = this.dirRelationships;
		} else {
			dir = MessageFormat.format("{0}/{1}", this.dirRelationships,
					filename);
		}

		if (!Hdfs.fileExists(dir))
			return sc.emptyRDD();

		return sc.textFile(dir).map(
				new Function<String, Edge<String>>() {
					public Edge<String> call(String line) throws Exception {
						// att: srcId, destId, value
						String[] att = line.split(SEPARATOR);

						Long srcId = Long.parseLong(att[0]);
						Long destId = Long.parseLong(att[1]);

						return new Edge<>(
								srcId,
								destId,
								att[2]);
					}
				});
	}

	/**
	 * Get the database instance of name name
	 * 
	 * @param name
	 * @return
	 * @throws NoSuchDatabaseException
	 * @throws IOException
	 */
	public static Database getInstance(String name) throws NoSuchDatabaseException, IOException {
		initConf();

		String n = name.toLowerCase();

		if (!db.containsKey(n)) {
			// Db exists on disk, create object and add to map
			if (getAll().contains(n))
				db.put(n, new Database(n));
			else
				throw new NoSuchDatabaseException(name);
		}

		return db.get(n);
	}

	private void setGraph(JavaRDD<Tuple2<Object, Node>> verticesRDD, JavaRDD<Edge<String>> edgesRDD) {
		this.graph = Graph.apply(
				verticesRDD.rdd(),
				edgesRDD.rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);
	}

	//////////////////// Database operations \\\\\\\\\\\\\\\\\\\\

	/**
	 * Create new database
	 * 
	 * @param name
	 * @return The newly created database
	 * @throws DatabaseExistsException
	 * @throws IOException
	 */
	public static Database create(String name) throws DatabaseExistsException, IOException {
		initConf();

		try {
			String dir = MessageFormat.format("{0}/{1}", DIR_DATABASES, name.toLowerCase());

			// Create db directory
			Hdfs.createDirectory(dir, false);

			// Create dir relationships
			Hdfs.createDirectory(
					MessageFormat.format("{0}/{1}", dir, RELATIONSHIPS_DIRECTORY_NAME),
					false);

			// Create dir nodetypes
			Hdfs.createDirectory(
					MessageFormat.format("{0}/{1}", dir, NODETYPES_DIRECTORY_NAME),
					false);

			// Create object and add to map
			Database newDb = new Database(name);
			db.put(name, newDb);
			return newDb;
		} catch (Exception e) {
			throw new DatabaseExistsException(name);
		}
	}

	/**
	 * Update database name
	 * 
	 * @param name
	 * @throws JobFailedException
	 */
	public void update(String name) throws JobFailedException {
		try {
			Hdfs.renameTo(
					this.dir,
					MessageFormat.format("{0}/{1}", DIR_DATABASES, name.toLowerCase()));
			this.setName(name);
		} catch (Exception e) {
			throw new JobFailedException(MessageFormat.format("Unable to update {0} database", this.name));
		}
	}

	/**
	 * Delete database
	 * 
	 * @throws JobFailedException
	 */
	public void delete() throws JobFailedException {
		try {
			Hdfs.delete(this.dir, true);
		} catch (Exception e) {
			throw new JobFailedException(MessageFormat.format("Unable to delete {0} database", this.name));
		}
	}

	//////////////////// Graph operations \\\\\\\\\\\\\\\\\\\\

	//////////////////// Nodes \\\\\\\\\\\\\\\\\\\\

	/**
	 * Add a node to the database
	 * 
	 * @param node
	 * @throws IOException
	 * @throws NoSuchNodeTypeException
	 */
	public void addNode(Node node) throws IOException, NoSuchNodeTypeException {
		if (!this.nodetypes.getAll().contains(node.getNodetype()))
			throw new NoSuchNodeTypeException(node.getNodetype().getName());

		// Link node to db and affect an id
		node.init(this.incrementAcc());

		// Append node to disk (throws err if fails, hence graph not modified if the
		// node is not added to disk first)
		appendNode(node);

		// Update graph with new node
		this.setGraph(
				this.graph.vertices().toJavaRDD()
						.union(sc.parallelize(Arrays.asList(new Tuple2<>(node.getUUID(), node)))),
				this.graph.edges().toJavaRDD());
	}

	/**
	 * Append a node to disk
	 * 
	 * @param node
	 * @throws IOException
	 */
	private void appendNode(Node node) throws IOException {
		String filename = this.getNodeFile(node);
		Hdfs.appendOrWrite(filename, node.toString());
	}

	/**
	 * Take a copy of an existing node (2 nodes are equals if they have the same
	 * id),
	 * with modified fields
	 * 
	 * @param node the node to update
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchNodeException
	 */
	public void updateNode(Node node)
			throws IllegalArgumentException, IOException, NoSuchNodeException {

		if (node.getUUID() == null)
			throw new IllegalArgumentException("Node must be added to the database before being updated");

		// Update node on disk
		this.updateNode(node, false);

		// Update node on graph
		this.setGraph(
				this.graph.vertices().toJavaRDD()
						.filter(v -> !v._2().equals(node))
						.union(sc.parallelize(Arrays.asList(new Tuple2<>(node.getUUID(), node)))),
				this.graph.edges().toJavaRDD());
	}

	/**
	 * Delete an existing node
	 * 
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchNodeException
	 */
	public void deleteNode(Node node) throws IllegalArgumentException, IOException, NoSuchNodeException {
		// Delete from disk
		this.updateNode(node, true);

		// Update graph without node
		this.setGraph(
				this.graph.vertices().toJavaRDD()
						.filter(v -> !v._2().equals(node)),
				this.graph.edges().toJavaRDD());
	}

	/**
	 * Update or delete a node from disk
	 * 
	 * @param node
	 * @param delete
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchNodeException
	 */
	private void updateNode(Node node, boolean delete)
			throws IllegalArgumentException, IOException, NoSuchNodeException {

		// Get content from file where the node is stored
		String filename = this.getNodeFile(node);

		String content;
		try {
			content = Hdfs.readFile(filename);
		} catch (Exception e) {
			throw new NoSuchNodeException(node.getUUID());
		}

		// Load content except the node to update / delete
		StringBuilder newContent = new StringBuilder();

		Long uuid;
		String[] lines = content.split("\n");
		for (int i = 0; i < lines.length; i++) {
			uuid = Long.parseLong(lines[i].split(SEPARATOR)[0]);

			if (!uuid.equals(node.getUUID())) {
				newContent.append(lines[i]);
				if (i != lines.length - 1)
					newContent.append("\n");
			}
		}

		// If update, add the new value
		if (!delete)
			newContent.append("\n" + node.toString());

		// Remove file
		Hdfs.delete(filename, true);

		// Write new content
		Hdfs.writeFile(filename, newContent.toString());
	}
	//////////////////// Relationships \\\\\\\\\\\\\\\\\\\\

	/**
	 * Add a relationship between 2 nodes of the graph
	 * 
	 * @param relationship
	 * @throws IOException
	 */
	public void addRelationship(Edge<String> relationship) throws IOException {
		// Add relationship to disk
		appendRelationship(relationship);

		// Update graph with new relationship
		this.setGraph(
				this.graph.vertices().toJavaRDD(),
				this.graph.edges().toJavaRDD().union(sc.parallelize(Arrays.asList(relationship))));
	}

	/**
	 * Append a relationship to disk
	 * 
	 * @param node
	 * @throws IOException
	 */
	private void appendRelationship(Edge<String> relationship) throws IOException {
		String filename = this.getRelationshipFile(relationship);
		Hdfs.appendOrWrite(filename, relationshipToString(relationship));
	}

	/**
	 * Update a relationship
	 *
	 * @param relationship The relationship to update
	 * @param newValue     The newValue of the relationship
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchRelationshipException
	 */
	public void updateRelationship(Edge<String> relationship, String newValue)
			throws IllegalArgumentException, IOException, NoSuchRelationshipException {

		String oldValue = relationship.attr();

		// Update relationship on disk (it updates the relationship.attr value with the
		// new value)
		this.updateRel(relationship, newValue);

		// Update graph
		this.setGraph(
				this.graph.vertices().toJavaRDD(),
				this.graph.edges().toJavaRDD()
						.filter(e -> (e.srcId() != relationship.srcId())
								|| (e.dstId() != relationship.dstId())
								|| !e.attr().equals(oldValue))
						.union(sc.parallelize(Arrays.asList(relationship))));
	}

	/**
	 * Delete an existing relationship
	 *
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchRelationshipException
	 */
	public void deleteRelationship(Edge<String> relationship) throws IllegalArgumentException,
			IOException, NoSuchRelationshipException {

		// Delete from disk
		this.updateRel(relationship, null);

		// Delete from graph
		this.setGraph(
				this.graph.vertices().toJavaRDD(), this.graph.edges().toJavaRDD()
						.filter(e -> (e.srcId() != relationship.srcId())
								|| (e.dstId() != relationship.dstId())
								|| !e.attr().equals(relationship.attr())));

	}

	/**
	 * Update or delete a relationship from disk
	 * 
	 * @param relationship
	 * @param newValue     The new value of the relationship (if null => delete
	 *                     relationship)
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchRelationshipException
	 */
	private void updateRel(Edge<String> relationship, String newValue)
			throws IllegalArgumentException, IOException, NoSuchRelationshipException {

		// Get content from file where the relationship is stored
		String filename = this.getRelationshipFile(relationship);
		String content;
		try {
			content = Hdfs.readFile(filename);
		} catch (Exception e) {
			throw new NoSuchRelationshipException(relationship);
		}

		// Load content except the relationship to update / delete
		StringBuilder newContent = new StringBuilder();

		Long src, dest;
		String[] lines = content.split("\n");
		String[] elems;
		for (int i = 0; i < lines.length; i++) {
			elems = lines[i].split(SEPARATOR);
			src = Long.parseLong(elems[0]);
			dest = Long.parseLong(elems[1]);

			if (src != relationship.srcId() || dest != relationship.dstId() || !elems[2].equals(relationship.attr())) {
				newContent.append(lines[i]);
				if (i != lines.length - 1)
					newContent.append("\n");
			}
		}

		// If update, add the new value
		if (newValue != null) {
			relationship.attr = newValue;
			newContent.append("\n" + relationshipToString(relationship));
		}

		// Remove file
		Hdfs.delete(filename, true);

		// Write new content
		Hdfs.writeFile(filename, newContent.toString());
	}

	//////////////////// Getters and Setters \\\\\\\\\\\\\\\\\\\\

	/**
	 * Set the name and directory of the database (based on its name)
	 * 
	 * @param name The name of the database
	 */
	private void setName(String name) {
		this.name = name.toLowerCase();
		this.dir = MessageFormat.format("{0}/{1}", DIR_DATABASES, this.name);
		this.dirNodetypes = MessageFormat.format("{0}/{1}", this.dir, NODETYPES_DIRECTORY_NAME);
		this.dirRelationships = MessageFormat.format("{0}/{1}", this.dir, RELATIONSHIPS_DIRECTORY_NAME);
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
	 * @throws IOException
	 */
	public static List<String> getAll() throws IOException {
		initConf();
		return Hdfs.listDirectories(DIR_DATABASES);
	}

	/**
	 * @return the graph
	 */
	public Graph<Node, String> getGraph() {
		return graph;
	}

	/**
	 * @return the available nodetypes of the database
	 */
	public NodeTypes getNodetypes() {
		return nodetypes;
	}

	/**
	 * @return the dirNodetypes
	 */
	protected String getDirNodetypes() {
		return dirNodetypes;
	}

	/**
	 * Get the absolute path of the node, based on its uuid
	 * 
	 * @param node
	 * @return
	 */
	private String getNodeFile(Node node) {
		return this.dirNodetypes + "/" + node.getNodetype().getName() + "/"
				+ Files.getNodeFile(node.getUUID());
	}

	/**
	 * Get the absolute path of the relationship, base on the source Node UUID
	 * 
	 * @param relationship
	 * @return
	 */
	private String getRelationshipFile(Edge<String> relationship) {
		return this.dirRelationships + "/" + Files.getRelationFile(relationship.srcId());
	}

	//////////////////// Accumulator \\\\\\\\\\\\\\\\\\\\

	/**
	 * Init the accumulator with the maximum uuid
	 * foreach nodetype, get last file, Get max id per file and init with max value
	 * 
	 * Since filenames are linked to uuid, and that the last file (file-n, where n
	 * is the bigger) contains the bigger UUID for the nodetype, we just have to
	 * check the last file of each nodetype, and get the maximum uuid.
	 * 
	 */
	private void initAcc() {
		this.nodetypes.getAll().forEach(nodetype -> {
			try {
				String maxFilename = Collections.max(Hdfs.listFiles(this.dirNodetypes + "/" + nodetype.getName()));

				// Get max uuid for given nodetype
				// Since each file has a given number of max elements (relatively low), it fits
				// in the driver's memory, hence can collect
				Long maxID = Collections.max(loadVertices(nodetype, maxFilename).map(x -> x._2()).collect()).getUUID();

				// set the accumulator value
				if (Long.compare(maxID, this.acc.value()) > 0)
					this.acc.setValue(maxID);

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Increment the accumulator and return its value (sum of added values)
	 * 
	 * @return
	 */
	private long incrementAcc() {
		this.acc.add(1L);
		return this.acc.sum();
	}

	//////////////////// Others \\\\\\\\\\\\\\\\\\\\

	/**
	 * Close spark context when done with the database
	 * 
	 * @throws IOException
	 */
	public void closeContext() {
		sc.close();
	}

	/**
	 * Edge<String> toString override
	 * 
	 * @param relationship
	 * @return
	 */
	private static String relationshipToString(Edge<String> relationship) {
		return relationship.srcId() + SEPARATOR + relationship.dstId() + SEPARATOR + relationship.attr;
	}
}
