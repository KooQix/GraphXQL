package dev.kooqix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.graphx.Edge;
import org.apache.spark.graphx.Graph;
import org.apache.spark.graphx.GraphLoader;
import org.apache.spark.storage.StorageLevel;

import scala.Tuple2;
import scala.reflect.ClassTag;

class Vertex implements Serializable {
	private String name;
	private String surname;

	private Long id;

	public Vertex(String name, String surname) {
		this.name = name;
		this.surname = surname;
		this.id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
	}

	public Vertex(Long id, String name, String surname) {
		this.name = name;
		this.surname = surname;
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}

	@Override
	public String toString() {
		return this.id + "--;--" + this.name + "--;--" + this.surname;
	}
}

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		final SparkConf conf = new SparkConf().setAppName("GraphXQL");
		JavaSparkContext sc = new JavaSparkContext(conf);

		// SparkContext sc = new SparkContext(conf);

		//////////////////// Test \\\\\\\\\\\\\\\\\\\\

		// //////////////////// Create and save \\\\\\\\\\\\\\\\\\\\

		// List<Tuple2<Object, String>> listOfVertex = new ArrayList<>();
		// listOfVertex.add(new Tuple2<>(1l, "James"));
		// listOfVertex.add(new Tuple2<>(2l, "Andy"));
		// listOfVertex.add(new Tuple2<>(3l, "Ed"));
		// listOfVertex.add(new Tuple2<>(4l, "Roger"));
		// listOfVertex.add(new Tuple2<>(5l, "Tony"));

		// List<Edge<String>> listOfEdge = new ArrayList<>();
		// listOfEdge.add(new Edge<>(2, 1, "Friend"));
		// listOfEdge.add(new Edge<>(3, 1, "Friend"));
		// listOfEdge.add(new Edge<>(3, 2, "Colleague"));
		// listOfEdge.add(new Edge<>(3, 5, "Partner"));
		// listOfEdge.add(new Edge<>(4, 3, "Boss"));
		// listOfEdge.add(new Edge<>(5, 2, "Partner"));

		// JavaRDD<Tuple2<Object, String>> vertexRDD = sc.parallelize(listOfVertex);
		// JavaRDD<Edge<String>> edgeRDD = sc.parallelize(listOfEdge);

		// ClassTag<String> stringTag =
		// scala.reflect.ClassTag$.MODULE$.apply(String.class);

		// Graph<String, String> graph = Graph.apply(
		// vertexRDD.rdd(),
		// edgeRDD.rdd(),
		// "",
		// StorageLevel.MEMORY_ONLY(),
		// StorageLevel.MEMORY_ONLY(),
		// stringTag,
		// stringTag);

		// // apply specific algorithms, such as PageRank

		// // graph.vertices()
		// // .saveAsTextFile("/home/vertices");

		// // graph.edges()
		// // .saveAsTextFile("/home/edges");

		// graph.vertices().toJavaRDD().coalesce(1).saveAsTextFile("/home/vertices");
		// graph.edges().toJavaRDD().coalesce(1).saveAsTextFile("/home/edges");

		// //////////////////// Load \\\\\\\\\\\\\\\\\\\\

		//////////////////// Create and save \\\\\\\\\\\\\\\\\\\\

		List<Tuple2<Object, Vertex>> listOfVertex = new ArrayList<>();
		Vertex v1 = new Vertex("name1", "surname1");
		Vertex v2 = new Vertex("name2", "surname2");
		Vertex v3 = new Vertex("name3", "surname3");
		Vertex v4 = new Vertex("name4", "surname4");

		listOfVertex.add(new Tuple2<>(v1.getId(), v1));
		listOfVertex.add(new Tuple2<>(v2.getId(), v2));
		listOfVertex.add(new Tuple2<>(v3.getId(), v3));
		listOfVertex.add(new Tuple2<>(v4.getId(), v4));

		List<Edge<String>> listOfEdge = new ArrayList<>();
		listOfEdge.add(new Edge<>(v1.getId(), v2.getId(), "Friend"));
		listOfEdge.add(new Edge<>(v2.getId(), v3.getId(), "Friend"));
		listOfEdge.add(new Edge<>(v3.getId(), v4.getId(), "Friend"));
		listOfEdge.add(new Edge<>(v4.getId(), v1.getId(), "Friend"));

		JavaRDD<Tuple2<Object, Vertex>> vertexRDD = sc.parallelize(listOfVertex);
		JavaRDD<Edge<String>> edgeRDD = sc.parallelize(listOfEdge);

		ClassTag<Vertex> vertexTag = scala.reflect.ClassTag$.MODULE$.apply(Vertex.class);
		ClassTag<String> stringTag = scala.reflect.ClassTag$.MODULE$.apply(String.class);

		Graph<Vertex, String> graph = Graph.apply(
				vertexRDD.rdd(),
				edgeRDD.rdd(),
				new Vertex("name", "surname"),
				StorageLevel.MEMORY_ONLY(),
				StorageLevel.MEMORY_ONLY(),
				vertexTag,
				stringTag);

		// graph.vertices().toJavaRDD().coalesce(1).saveAsTextFile("/home/vertices");
		// // graph.edges().toJavaRDD().coalesce(1).saveAsTextFile("/home/edges");

		graph.vertices().toJavaRDD().map(x -> x._2()).saveAsTextFile("/home/vertices");
		graph.edges().toJavaRDD().map(x -> x.srcId() + "--;--" + x.dstId() + "--;--" +
				x.attr())
				.saveAsTextFile("/home/edges");

		//////////////////// Load \\\\\\\\\\\\\\\\\\\\

		JavaRDD<Tuple2<Object, Vertex>> vert = sc.textFile("/home/vertices").map(
				new Function<String, Tuple2<Object, Vertex>>() {
					public Tuple2<Object, Vertex> call(String line) throws Exception {
						String[] attributes = line.split("--;--");

						Vertex v = new Vertex(Long.parseLong(attributes[0]),
								attributes[1],
								attributes[2]);
						return new Tuple2<>(v.getId(), v);
					}
				});

		JavaRDD<Edge<String>> edges = sc.textFile("/home/edges").map(
				new Function<String, Edge<String>>() {
					public Edge<String> call(String line) throws Exception {
						String[] att = line.split("--;--");
						return new Edge<>(Long.parseLong(att[0]), Long.parseLong(att[1]), att[2]);
					}
				});

		ClassTag<Vertex> vertexTag2 = scala.reflect.ClassTag$.MODULE$.apply(Vertex.class);
		ClassTag<String> stringTag2 = scala.reflect.ClassTag$.MODULE$.apply(String.class);

		Graph<Vertex, String> graph2 = Graph.apply(
				vert.rdd(),
				edges.rdd(),
				new Vertex("name", "surname"),
				StorageLevel.MEMORY_ONLY(),
				StorageLevel.MEMORY_ONLY(),
				vertexTag2,
				stringTag2);

		graph2.vertices().toJavaRDD().map(x -> x._2()).saveAsTextFile("/home/2/vertices");
		graph2.edges().toJavaRDD().map(x -> x.srcId() + "--;--" + x.dstId() + "--;--" +
				x.attr())
				.saveAsTextFile("/home/2/edges");

		sc.close();
	}
}

// https://spark.apache.org/docs/1.3.1/sql-programming-guide.html