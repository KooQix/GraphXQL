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

import com.esotericsoftware.minlog.Log;

import dev.kooqix.exceptions.DatabaseExistsException;
import dev.kooqix.exceptions.JobFailedException;
import dev.kooqix.exceptions.NoSuchDatabaseException;
import scala.Option;
import scala.Tuple2;
import scala.reflect.ClassTag;

public class Database implements Serializable {

	private static final String APP_NAME = "GraphXQL";
	private static String SEPARATOR = "--;--";

	//////////////////// Configuration variables \\\\\\\\\\\\\\\\\\\\

	private static String GRAPHXQL_HOME;

	private static final String RELATIONSHIPS_DIRECTORY_NAME = "relationships";
	private static final String NODETYPES_DIRECTORY_NAME = "nodetypes";

	private static String DIR_DATABASES;

	//////////////////// Spark context \\\\\\\\\\\\\\\\\\\\

	private static final SparkConf conf = new SparkConf().setAppName(APP_NAME);
	private static JavaSparkContext sc;

	private static boolean confInit = false;

	//////////////////// Attributes \\\\\\\\\\\\\\\\\\\\

	private String dir;
	private String dirNodetypes;
	private String dirRelationships;
	private String name;
	private NodeTypes nodetypes;
	private Graph<Node, String> graph;

	private LongAccumulator acc;

	private static ClassTag<Node> vertexTag = scala.reflect.ClassTag$.MODULE$.apply(Node.class);
	private static ClassTag<String> edgesTag = scala.reflect.ClassTag$.MODULE$.apply(String.class);

	// Open databases (multiton, 1 singleton per database to ensure consistency)
	private static Map<String, Database> db = new HashMap<>();

	/**
	 * Get Spark configuration variables
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

						Node node = new Node(nodetype);

						for (i = 1; i < attributes.length; i++) {
							field = attributes[i].split(Field.getSeparator());
							try {
								node.addField(new Field(field[0], field[1]));
							} catch (Exception e) {
							}
						}
						node.init(Long.parseLong(attributes[0]));

						return new Tuple2<>(node.getUUID(), node);
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

	// public void save() throws IOException {
	// Hdfs.deleteUnder(this.dirRelationships);

	// //////////////////// Save nodes \\\\\\\\\\\\\\\\\\\\

	// // this.nodetypes.getAll().forEach(
	// // type -> {
	// // try {
	// // Hdfs.deleteUnder(this.dirNodetypes + "/" + type.getName());
	// // } catch (Exception e) {
	// // }

	// // this.graph.vertices().toJavaRDD()
	// // .filter(elem -> elem._2().getNodetype().equals(type))
	// // .flatMap(x -> Arrays.asList(x._2()).iterator())
	// // .groupBy(x -> Files.getNodeFile(x.getUUID()))
	// // .foreach(elem -> elem._2().forEach(
	// // node -> {
	// // try {
	// // Hdfs.appendOrWrite(
	// // this.dirNodetypes + "/" + type.getName() + "/"
	// // + elem._1(),
	// // node.toString());
	// // } catch (Exception e) {
	// // }
	// // }));
	// // });

	// this.graph.edges().toJavaRDD()
	// .groupBy(x -> Files.getRelationFile(x.srcId()))
	// .foreach(elem -> elem._2().forEach(rel -> {
	// try {
	// Hdfs.appendOrWrite(this.dirRelationships + "/" + elem._1(),
	// relationshipToString(rel));
	// } catch (Exception e) {
	// }
	// }));
	// }

	//////////////////// Nodes \\\\\\\\\\\\\\\\\\\\

	/**
	 * Add a node to the database
	 * 
	 * @param node
	 * @throws IOException
	 */
	public void addNode(Node node) throws IOException {
		// Link node to db and affect an id
		node.init(this.incrementAcc());

		// Append node to disk (throws err if fails, hence graph not modified if the
		// node is not added to disk first)
		appendNode(node);

		// Update graph with new node
		this.graph = Graph.apply(
				this.graph.vertices().toJavaRDD()
						.union(sc.parallelize(Arrays.asList(new Tuple2<>(node.getUUID(), node)))).rdd(),
				this.graph.edges().toJavaRDD().rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);

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
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void updateNode(Node updatedNode, long nodeUUID) throws IllegalArgumentException, IOException {
		updatedNode.init(nodeUUID);
		this.updateNode(updatedNode, false);
	}

	/**
	 * Delete an existing node
	 * 
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void deleteNode(Node node) throws IllegalArgumentException, IOException {
		this.updateNode(node, true);
	}

	/**
	 * Update or delete a node from disk
	 * 
	 * @param node
	 * @param delete
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private void updateNode(Node node, boolean delete) throws IllegalArgumentException, IOException {

		// Get content from file where the node is stored
		JavaRDD<Tuple2<Object, Node>> content = this.loadVertices(node.getNodetype(),
				Files.getNodeFile(node.getUUID())).filter(x -> !(x._2().equals(node)));

		String filename = this.getNodeFile(node);

		// Remove file
		Hdfs.delete(filename, true);

		// Update the previous content with the new node value
		if (!delete) {
			content = content.union(sc.parallelize(Arrays.asList(new Tuple2<>(node.getUUID(), node))));
		}

		// Write the new content back
		content.foreach(x -> Hdfs.appendOrWrite(filename, x._2().toString()));

	}

	//////////////////// Relationships \\\\\\\\\\\\\\\\\\\\

	public void addRelationship(Edge<String> relationship) throws IOException {
		appendRelationship(relationship);

		this.graph = Graph.apply(
				this.graph.vertices().toJavaRDD().rdd(),
				this.graph.edges().toJavaRDD().union(sc.parallelize(Arrays.asList(relationship))).rdd(),
				null,
				StorageLevel.MEMORY_AND_DISK(),
				StorageLevel.MEMORY_AND_DISK(),
				vertexTag,
				edgesTag);
	}

	/**
	 * Append a node to disk
	 * 
	 * @param node
	 * @throws IOException
	 */
	private void appendRelationship(Edge<String> relationship) throws IOException {
		String filename = this.getRelationshipFile(relationship);
		Hdfs.appendOrWrite(filename, relationshipToString(relationship));
	}

	/**
	 * Take a copy of an existing node (2 nodes are equals if they have the same
	 * id),
	 * with modified fields
	 *
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void updateRelationship(Edge<String> relationship, String newValue)
			throws IllegalArgumentException, IOException {
		this.updateRel(relationship, newValue);
	}

	/**
	 * Delete an existing node
	 *
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public void deleteRelationship(Edge<String> relationship) throws IllegalArgumentException,
			IOException {
		this.updateRel(relationship, null);
	}

	/**
	 * Update or delete a relationship from disk
	 * 
	 * @param relationship
	 * @param delete
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private void updateRel(Edge<String> relationship, String newValue)
			throws IllegalArgumentException, IOException {

		// Get content from file where the node is stored
		JavaRDD<Edge<String>> content = this.loadEdges(Files.getRelationFile(relationship.srcId()))
				.filter(x -> (x.srcId() != relationship.srcId()) || (x.dstId() != relationship.dstId()));

		String filename = this.getRelationshipFile(relationship);

		// Remove file
		Hdfs.delete(filename, true);

		// Update the previous content with the new relationship value
		if (newValue != null) {
			content = content.union(sc.parallelize(
					Arrays.asList(new Edge<>(relationship.srcId(), relationship.dstId(), newValue))));
		}

		// Write the new content back
		content.foreach(x -> Hdfs.appendOrWrite(filename, relationshipToString(x)));

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
	 * @return the nodetypes
	 */
	public NodeTypes getNodetypes() {
		return nodetypes;
	}

	/**
	 * @return the dirNodetypes
	 */
	public String getDirNodetypes() {
		return dirNodetypes;
	}

	private String getNodeFile(Node node) {
		return this.dirNodetypes + "/" + node.getNodetype().getName() + "/"
				+ Files.getNodeFile(node.getUUID());
	}

	private String getRelationshipFile(Edge<String> relationship) {
		return this.dirRelationships + "/" + Files.getRelationFile(relationship.srcId());
	}

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

				if (Long.compare(maxID, this.acc.value()) > 0)
					this.acc.setValue(maxID);

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private long incrementAcc() {
		this.acc.add(1L);
		return this.acc.sum();
	}

	//////////////////// Others \\\\\\\\\\\\\\\\\\\\

	public void closeContext() throws IOException {
		sc.close();
	}

	@Override
	public String toString() {
		return MessageFormat.format("{0}\n\t{1} nodetypes", this.name.toUpperCase(), this.nodetypes.getAll().size());
	}

	private static String relationshipToString(Edge<String> relationship) {
		return relationship.srcId() + SEPARATOR + relationship.dstId() + SEPARATOR + relationship.attr;
	}
}
