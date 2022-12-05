package dev.kooqix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.spark.graphx.Edge;

import com.esotericsoftware.minlog.Log;

import dev.kooqix.exceptions.InalterableNodeException;
import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.graphxql.Database;
import dev.kooqix.graphxql.Field;
import dev.kooqix.graphxql.Node;
import dev.kooqix.graphxql.NodeType;

/**
 * Hello world!
 *
 */
public class App {

	public static void testLoading(Database db) {
		Log.info("\n\nVertices:");
		List<Node> nodes = db.getGraph().vertices().toJavaRDD().map(x -> x._2()).collect();
		for (Node node : nodes)
			Log.info(node.toString());

		Log.info("\n\nEdges:");
		List<Edge<String>> relationships = db.getGraph().edges().toJavaRDD().map(x -> x).collect();

		for (Edge<String> relationship : relationships)
			Log.info(relationship.toString());
	}

	public static void testCreateNew(Database db, int numUsers)
			throws IOException, IllegalArgumentException, InalterableNodeException {
		NodeType userNodeType = new NodeType("User");
		db.getNodetypes().addNodeType(userNodeType);

		// Create nodes
		List<Node> nodes = new ArrayList<>();
		for (int i = 1; i < numUsers + 1; i++) {
			Node user = new Node(userNodeType);
			user.addField(new Field<String>("name", "user" + i));
			user.addField(new Field<Integer>("favorite_number", i));
			nodes.add(user);

			// Add nodes to graph
			db.addNode(user);
		}

		List<Edge<String>> relationships = new ArrayList<>();
		for (int i = 0; i < numUsers - 2; i++) {
			Edge<String> rel = new Edge<>(nodes.get(i).getUUID(), nodes.get(i + 1).getUUID(), "Friends");

			relationships.add(rel);

			// Add relationship
			db.addRelationship(rel);
		}

		// db.save();
	}

	public static void main(String[] args)
			throws NoSuchDatabaseException, IOException, IllegalArgumentException, InalterableNodeException {

		//////////////////// Test db \\\\\\\\\\\\\\\\\\\\

		Database db;
		String dbName = "newdb";

		try {
			db = Database.create(dbName);
		} catch (Exception e) {
			db = Database.getInstance(dbName);
		}

		// Create nodetype
		// testCreateNew(db, 10);

		//////////////////// Try loading \\\\\\\\\\\\\\\\\\\\

		testLoading(db);

		db.closeContext();
		Log.info("Done!");
	}
}
