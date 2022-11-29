package dev.kooqix;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.EdgeRDD;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.PartitionStrategy;
import org.apache.spark.graphx.TripletFields;
import org.apache.spark.graphx.VertexRDD;
import org.apache.spark.rdd.RDD;
import org.apache.spark.storage.StorageLevel;

import dev.kooqix.exceptions.DatabaseExistsException;
import dev.kooqix.exceptions.JobFailedException;
import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.node.Node;
import dev.kooqix.node.NodeType;
import dev.kooqix.relationships.Relationship;
import scala.Tuple2;
import scala.reflect.ClassTag;

public class Database {

	private static final String APP_NAME = "GraphXQL";

	//////////////////// Configuration variables \\\\\\\\\\\\\\\\\\\\

	private static final String GRAPHXQL_HOME = System.getenv("GRAPHXQL_HOME");

	private static final String RELATIONSHIPS_FILE = "relationships.parquet";
	private static final String NODETYPES_DIRECTORY_NAME = "nodetypes";
	private static final String DIR_DATABASES = MessageFormat.format("hdfs://{0}/databases",
			GRAPHXQL_HOME);

	//////////////////// Spark context \\\\\\\\\\\\\\\\\\\\

	private static final SparkConf conf = new SparkConf().setAppName(APP_NAME);
	private static JavaSparkContext sc = new JavaSparkContext(conf);

	//////////////////// Attributes \\\\\\\\\\\\\\\\\\\\

	private String dir;
	private String name;
	private Collection<NodeType> nodetypes;
	private Graph<Node, Relationship> graph;

	// Open databases (multiton, 1 singleton per database to ensure consistency)
	private static Map<String, Database> db = new HashMap<String, Database>();

	/**
	 * Load database
	 * 
	 * @param name
	 */
	private Database(String name) {
		this.setName(name);

		// Load available nodetypes
		NodeType.load(MessageFormat.format("{0}/{1}", this.dir, NODETYPES_DIRECTORY_NAME));
		this.nodetypes = NodeType.getAll();

		if (!this.nodetypes.isEmpty()) {
			// Load vertices for all nodetypes.... for now, the first
			JavaRDD<Tuple2<Object, Node>> verticesRDD = this.loadVertices(this.nodetypes.iterator().next());
			JavaRDD<Edge<Relationship>> edgesRDD = this.loadEdges(verticesRDD);

			// Load graph
			ClassTag<Node> vertexTag = scala.reflect.ClassTag$.MODULE$.apply(Node.class);
			ClassTag<Relationship> edgesTag = scala.reflect.ClassTag$.MODULE$.apply(Relationship.class);

			this.graph = Graph.apply(
					verticesRDD.rdd(),
					edgesRDD.rdd(),
					null,
					StorageLevel.MEMORY_AND_DISK(),
					StorageLevel.MEMORY_AND_DISK(),
					vertexTag,
					edgesTag);
		}
	}

	private JavaRDD<Tuple2<Object, Node>> loadVertices(NodeType nodetype) {
		return sc.textFile(MessageFormat.format("{0}/vertices", this.dir)).map(
				new Function<String, Tuple2<Object, Node>>() {
					public Tuple2<Object, Node> call(String line) throws Exception {
						// uuid, content
						String[] attributes = line.split("--;--");

						Node node = new Node(nodetype, Long.parseLong(attributes[0]), attributes[1]);

						return new Tuple2<>(node.getUUId(), node);
					}
				});
	}

	private JavaRDD<Edge<Relationship>> loadEdges(JavaRDD<Tuple2<Object, Node>> vertices) {
		return sc.textFile(MessageFormat.format("{0}/edges", this.dir)).map(
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
	 */
	public static Database getInstance(String name) throws NoSuchDatabaseException {
		if (!db.containsKey(name)) {
			// Db exists, add to map
			if (getAll().contains(name))
				db.put(name, new Database(name));
			else
				throw new NoSuchDatabaseException(name);
		}

		return db.get(name);
	}

	//////////////////// Database operations \\\\\\\\\\\\\\\\\\\\

	/**
	 * Create new database
	 * 
	 * @param name
	 * @return The newly created database
	 * @throws DatabaseExistsException
	 */
	public static Database create(String name) throws DatabaseExistsException {

		try {
			// Create db directory
			new File(MessageFormat.format("{0}/{1}", DIR_DATABASES, name.toLowerCase())).mkdirs();

			// Create files
			String dir = MessageFormat.format("{0}/{1}", DIR_DATABASES, name.toLowerCase());

			new File(MessageFormat.format("{0}/{1}", dir, RELATIONSHIPS_FILE));
			new File(MessageFormat.format("{0}/{1}", dir, NODETYPES_DIRECTORY_NAME)).mkdirs();

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
		File dbDir = new File(this.dir);

		if (dbDir.renameTo(new File(MessageFormat.format("{0}/{1}", DIR_DATABASES, this.name))))
			this.setName(name);
		else
			throw new JobFailedException(MessageFormat.format("Unable to update {0} database", this.name));

	}

	public void delete() {
		// TODO
		// Delete all directory
	}

	public void save() {
		this.graph.vertices().toJavaRDD().map(x -> x._2())
				.saveAsTextFile(MessageFormat.format("{0}/graphVertices", this.dir));
		this.graph.edges().toJavaRDD().map(x -> x.attr())
				.saveAsTextFile(MessageFormat.format("{0}/graphEdges", this.dir));
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

		List<String> databases = new ArrayList<>();
		String[] path;
		for (File file : directories) {
			path = file.getPath().split("/");
			databases.add(path[path.length - 1]);
		}
		return databases;
	}

	/**
	 * @return the graph
	 */
	public Graph<Node, Relationship> getGraph() {
		return graph;
	}

	//////////////////// Others \\\\\\\\\\\\\\\\\\\\

	public static void closeContext() {
		sc.close();
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}\n\t{1} nodetypes", this.name.toUpperCase(), this.nodetypes.size());
	}
}
