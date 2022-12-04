package dev.kooqix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.spark.graphx.Edge;

import com.esotericsoftware.minlog.Log;

import dev.kooqix.exceptions.NoSuchDatabaseException;
import dev.kooqix.graphxql.Database;
import dev.kooqix.graphxql.Field;
import dev.kooqix.graphxql.Node;
import dev.kooqix.graphxql.NodeType;
import scala.Tuple2;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) throws NoSuchDatabaseException, IOException {

		//////////////////// Test db \\\\\\\\\\\\\\\\\\\\

		Database db;
		String dbName = "newdb";

		try {
			db = Database.create(dbName);
		} catch (Exception e) {
			db = Database.getInstance(dbName);
		}

		// // Create nodetype
		// NodeType user = new NodeType("User");
		// db.getNodetypes().addNodeType(user);

		// // Create nodes
		// Node user1 = new Node(user);
		// Node user2 = new Node(user);

		// user1.addField(new Field<String>("name", "user1"));
		// user1.addField(new Field<Integer>("favorite_number", 1));

		// user2.addField(new Field<String>("name", "user2"));
		// user2.addField(new Field<Integer>("favorite_number", 2));

		// List<Node> nodes = new ArrayList<>();
		// nodes.add(user1);
		// nodes.add(user2);

		// // Add nodes to graph
		// db.addNodes(nodes);

		// // Add relationship
		// Relationship rel1 = new Relationship(user1.getUUId(), user2.getUUId(),
		// "Friends");
		// db.addRelationship(rel1);

		// db.save();

		Log.info("\n\nVertices:");
		List<Node> nodes = db.getGraph().vertices().toJavaRDD().map(x -> x._2()).collect();
		for (Node node : nodes)
			Log.info(node.toString());

		Log.info("\n\nEdges:");
		List<Edge<String>> relationships = db.getGraph().edges().toJavaRDD().map(x -> x).collect();

		for (Edge<String> relationship : relationships)
			Log.info(relationship.toString());

		db.closeContext();
		Log.info("Done!");
	}
}
