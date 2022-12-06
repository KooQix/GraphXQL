package dev.kooqix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.graphx.Edge;

import com.esotericsoftware.minlog.Log;

import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.exceptions.NoSuchNodeException;
import dev.kooqix.exceptions.NoSuchNodeTypeException;
import dev.kooqix.exceptions.NoSuchRelationshipException;
import dev.kooqix.graphxql.Database;
import dev.kooqix.graphxql.Field;
import dev.kooqix.graphxql.Node;
import dev.kooqix.graphxql.NodeType;

/**
 * Hello world!
 *
 */
public class App {

	//////////////////// Some tests \\\\\\\\\\\\\\\\\\\\

	/**
	 * Create some users and relationships
	 * 
	 * @param db
	 * @param numUsers
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws NoSuchNodeTypeException
	 * @throws ImmutableNodeException
	 */
	public static void testCreateNew(Database db, int numUsers)
			throws IOException, IllegalArgumentException, NoSuchNodeTypeException {
		Log.info("\n\n--------------------- TEST CREATE ---------------------\n\n");

		// Nodetypes are saved as lowercase
		NodeType userNodeType = new NodeType("User");
		// Add to database
		db.getNodetypes().addNodeType(userNodeType);

		// Create nodes
		List<Node> nodes = new ArrayList<>();
		for (int i = 1; i < numUsers + 1; i++) {
			Node user = new Node(userNodeType);

			// Add fields to user
			user.addField(new Field<String>("name", "user" + i));
			user.addField(new Field<Integer>("favorite number", i));
			nodes.add(user);

			// Add nodes to database
			db.addNode(user);
		}

		// Create some relationships
		for (int i = 0; i < numUsers - 2; i++) {
			String value = (i % 5 == 1) ? "Best friends" : "Friends";

			Edge<String> rel = new Edge<>(nodes.get(i).getUUID(), nodes.get(i + 1).getUUID(), value);

			// Add relationship to db
			db.addRelationship(rel);
		}
	}

	/**
	 * Display graph nodes and relationships
	 * 
	 * @param db
	 */
	public static void displayGraph(Database db) {
		Log.info("\n\n--------------------- TEST DISPLAY ---------------------\n\n");
		Log.info("\n\nVertices:");

		List<Node> nodes = db.getGraph().vertices().toJavaRDD().map(x -> x._2()).collect();
		for (Node node : nodes)
			Log.info(node.toString());

		Log.info("\n\nEdges:");
		List<Edge<String>> relationships = db.getGraph().edges().toJavaRDD().map(x -> x).collect();

		for (Edge<String> relationship : relationships)
			Log.info(relationship.toString());
	}

	/**
	 * Display fields of the first node
	 * 
	 * @param db
	 */
	public static void testFields(Database db) {
		Log.info("\n\n--------------------- TEST FIELDS ---------------------\n\n");

		Node node = db.getGraph().vertices().toJavaRDD().first()._2();
		Log.info("\nnode: " + node.getUUID());
		for (Field field : node.getFields()) {
			Log.info("\n\nfield: " + field.toString());
		}
	}

	/**
	 * Update one node
	 * 
	 * @param db
	 * @throws ImmutableNodeException
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchNodeException
	 */
	public static void testUpdateNode(Database db)
			throws IllegalArgumentException, IOException, NoSuchNodeException {
		Log.info("\n\n--------------------- TEST UPDATE NODE ---------------------\n\n");

		// Get a node that has the NodeType user
		Node node = db.getGraph().vertices().toJavaRDD()
				.filter(v -> v._2().getNodetype().getName().equals("user"))
				.first()._2();

		// We know each user has a name field (set will replace if key exists, add
		// otherwise)
		node.setField(new Field<String>("name", "updatedName"));

		// Update node
		// Since the graph is immutable, modifying the node will not affect the node on
		// the graph. To do so, we need to call db.updateNode and pass the node to
		// update (referenced via its uuid) as argument
		db.updateNode(node);
	}

	/**
	 * Update a relationship value
	 * 
	 * @param db
	 * @throws IllegalArgumentException
	 * @throws IOException
	 * @throws NoSuchRelationshipException
	 */
	public static void testUpdateRelationship(Database db)
			throws IllegalArgumentException, IOException, NoSuchRelationshipException {
		Log.info("\n\n--------------------- TEST UPDATE RELATIONSHIP ---------------------\n\n");

		Edge<String> rel = db.getGraph().edges().toJavaRDD().first();

		db.updateRelationship(rel, "Best friends");
	}

	/**
	 * Select users having a given relationship value
	 * 
	 * @param db
	 * @throws NoSuchFieldException
	 */
	public static void testSelect(Database db) throws NoSuchFieldException {
		String relValue = "Best friends";

		// Get relationships
		JavaRDD<Edge<String>> resRelations = db.getGraph().edges().toJavaRDD()
				.filter(edge -> edge.attr().equals(relValue));

		Log.info("\n\n" + resRelations.count() + " relations found\n");

		// Get users and display user1.name relValue user2.name
		for (Edge<String> relationship : resRelations.collect()) {
			Node src = db.getGraph().vertices().toJavaRDD().filter(v -> v._2().getUUID() == relationship.srcId())
					.first()._2();
			Node dest = db.getGraph().vertices().toJavaRDD().filter(v -> v._2().getUUID() == relationship.dstId())
					.first()._2();

			Log.info("\n\n" + src.getFieldValue("name") + " " + relValue + " with " + dest.getFieldValue("name")
					+ "\n\n");
		}
	}

	public static void main(String[] args)
			throws NoSuchDatabaseException, IOException, IllegalArgumentException,
			NoSuchRelationshipException, NoSuchNodeException, NoSuchFieldException, NoSuchNodeTypeException {

		//////////////////// Test db \\\\\\\\\\\\\\\\\\\\

		Database db;
		String dbName = "newdb";

		// Create if doesn't exist, else get
		try {
			db = Database.create(dbName);
		} catch (Exception e) {
			db = Database.getInstance(dbName);
		}

		// Create nodetype
		testCreateNew(db, 10);

		displayGraph(db);

		testSelect(db);

		// Updates
		testUpdateNode(db);

		testUpdateRelationship(db);

		// Display updates
		displayGraph(db);

		// Add new nodes
		testCreateNew(db, 20);

		// display new graph
		displayGraph(db);

		db.closeContext();
		Log.info("Done!");
	}
}
