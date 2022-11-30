package dev.kooqix.database;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.Graph;
import org.apache.spark.storage.StorageLevel;

import com.esotericsoftware.minlog.Log;

import dev.kooqix.exceptions.DatabaseExistsException;
import dev.kooqix.exceptions.JobFailedException;
import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.node.Node;
import dev.kooqix.node.NodeType;
import dev.kooqix.relationships.Relationship;
import scala.Tuple2;
import scala.reflect.ClassTag;

public class Database implements Serializable {

	private static final String APP_NAME = "GraphXQL";

	//////////////////// Configuration variables \\\\\\\\\\\\\\\\\\\\

	private static String GRAPHXQL_HOME;

	private static final String RELATIONSHIPS_DIRECTORY_NAME = "relationships";
	private static final String RELATIONSHIPS_FILE = "relationships.parquet";
	private static final String NODETYPES_DIRECTORY_NAME = "nodetypes";

	private static String DIR_DATABASES;

	//////////////////// Spark context \\\\\\\\\\\\\\\\\\\\

	private static final SparkConf conf = new SparkConf().setAppName(APP_NAME);
	private JavaSparkContext sc;

	private static boolean confInit = false;

	//////////////////// Attributes \\\\\\\\\\\\\\\\\\\\

	private String dir;
	private String dirNodetypes;
	private String name;
	private NodeTypes nodetypes;
	private Graph<Node, Relationship> graph;

	private static ClassTag<Node> vertexTag = scala.reflect.ClassTag$.MODULE$.apply(Node.class);
	private static ClassTag<Relationship> edgesTag = scala.reflect.ClassTag$.MODULE$.apply(Relationship.class);

	// Open databases (multiton, 1 singleton per database to ensure consistency)
	private static Map<String, Database> db = new HashMap<>();

	/**
	 * Get Spark configuration variables
	 */
	private static void initConf() {
		if (!confInit) {
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
		sc = new JavaSparkContext(conf);
		initConf();

		this.setName(name);

		// Load available nodetypes, for the given database
		this.nodetypes = new NodeTypes(
				this,
				MessageFormat.format("{0}/{1}", this.dir, NODETYPES_DIRECTORY_NAME));

		JavaRDD<Tuple2<Object, Node>> verticesRDD;
		JavaRDD<Edge<Relationship>> edgesRDD;

		if (!this.nodetypes.getAll().isEmpty()) {
			// Load vertices for all nodetypes.... for now, the first
			verticesRDD = this.loadVertices(this.nodetypes.getAll().iterator().next());
			edgesRDD = this.loadEdges(verticesRDD);
		} else {
			verticesRDD = sc.emptyRDD();
			edgesRDD = sc.emptyRDD();
		}

		// Load graph
		this.graph = Graph.apply(
				verticesRDD.rdd(),
				edgesRDD.rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);

	}

	/**
	 * Load vertices from file
	 * 
	 * @param nodetype
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private JavaRDD<Tuple2<Object, Node>> loadVertices(NodeType nodetype) throws IllegalArgumentException, IOException {
		String dir = MessageFormat.format("{0}/{1}/vertices", this.dirNodetypes,
				nodetype.getName());
		if (!Hdfs.fileExists(dir))
			return sc.emptyRDD();

		return sc.textFile(dir).map(
				new Function<String, Tuple2<Object, Node>>() {
					public Tuple2<Object, Node> call(String line) throws Exception {
						// uuid, content
						String[] attributes = line.split("--;--");

						Node node = new Node(nodetype, Long.parseLong(attributes[0]), attributes[1]);

						return new Tuple2<>(node.getUUId(), node);
					}
				});
	}

	/**
	 * Load edges from file
	 * 
	 * @param vertices
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	private JavaRDD<Edge<Relationship>> loadEdges(JavaRDD<Tuple2<Object, Node>> vertices)
			throws IllegalArgumentException, IOException {
		String dir = MessageFormat.format("{0}/edges", this.dir);
		if (!Hdfs.fileExists(dir))
			return sc.emptyRDD();

		return sc.textFile(dir).map(
				new Function<String, Edge<Relationship>>() {
					public Edge<Relationship> call(String line) throws Exception {
						// att: srcId, destId, value
						String[] att = line.split("--;--");

						Long srcId = Long.parseLong(att[0]);
						Long destId = Long.parseLong(att[1]);

						Relationship rel = new Relationship(
								att[3],
								vertices.filter(v -> v._1().toString().equals(srcId.toString())).first()._2(),
								vertices.filter(v -> v._1().toString().equals(destId.toString())).first()._2());

						return new Edge<>(srcId, destId, rel);
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
			// Db exists, add to map
			if (getAll().contains(n))
				db.put(n, new Database(n));
			else
				throw new NoSuchDatabaseException(name);
		}

		return db.get(n);
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

			Log.info("Creating directory: " + dir);

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
	 * Update database
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

	public void delete() throws JobFailedException {
		try {
			Hdfs.delete(this.dir, true);
		} catch (Exception e) {
			throw new JobFailedException(MessageFormat.format("Unable to delete {0} database", this.name));
		}
	}

	public void save() throws IOException {
		String nodetypeName = this.nodetypes.getAll().iterator().next().getName();

		Hdfs.delete(MessageFormat.format("{0}/{1}/vertices",
				this.dirNodetypes, nodetypeName), true);

		Hdfs.delete(MessageFormat.format("{0}/edges",
				this.dir), true);

		this.graph.vertices().toJavaRDD().flatMap(x -> Arrays.asList(x._2()).iterator())
				.saveAsTextFile(MessageFormat.format("{0}/{1}/vertices",
						this.dirNodetypes, nodetypeName));

		this.graph.edges().toJavaRDD().flatMap(x -> Arrays.asList(x.attr).iterator())
				.saveAsTextFile(MessageFormat.format("{0}/edges",
						this.dir));

	}

	//////////////////// Graph operations \\\\\\\\\\\\\\\\\\\\

	public void addNode(Node node) {
		List<Tuple2<Object, Node>> l = new ArrayList<>();
		l.add(new Tuple2<>(node.getUUId(), node));

		this.graph = Graph.apply(
				this.graph.vertices().toJavaRDD().union(sc.parallelize(l)).rdd(),
				this.graph.edges().toJavaRDD().rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);

	}

	public void addNodes(List<Node> nodes) {
		List<Tuple2<Object, Node>> l = new ArrayList<>();
		for (Node node : nodes)
			l.add(new Tuple2<>(node.getUUId(), node));

		this.graph = Graph.apply(
				this.graph.vertices().toJavaRDD().union(sc.parallelize(l)).rdd(),
				this.graph.edges().toJavaRDD().rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);

	}

	public void addRelationship(Relationship relationship) {
		List<Edge<Relationship>> list = new ArrayList<>();
		list.add(new Edge<>(relationship.getNode1().getUUId(), relationship.getNode2().getUUId(), relationship));

		this.graph = Graph.apply(
				this.graph.vertices().toJavaRDD().rdd(),
				this.graph.edges().toJavaRDD().union(sc.parallelize(list)).rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);
	}

	public void addRelationships(List<Relationship> relationships) {
		List<Edge<Relationship>> list = new ArrayList<>();
		for (Relationship relationship : relationships)
			list.add(new Edge<>(relationship.getNode1().getUUId(), relationship.getNode2().getUUId(), relationship));

		this.graph = Graph.apply(
				this.graph.vertices().toJavaRDD().rdd(),
				this.graph.edges().toJavaRDD().union(sc.parallelize(list)).rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);
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
	public Graph<Node, Relationship> getGraph() {
		return graph;
	}

	/**
	 * @return the nodetypes
	 */
	public NodeTypes getNodetypes() {
		return nodetypes;
	}

	//////////////////// Others \\\\\\\\\\\\\\\\\\\\

	public void closeContext() throws IOException {
		sc.close();
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}\n\t{1} nodetypes", this.name.toUpperCase(), this.nodetypes.getAll().size());
	}
}
