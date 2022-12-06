package dev.kooqix.graphxql;

public class Files {
	// private static int MAX_NUM_NODE = 500;
	// private static int MAX_REL = 1000;

	// Set max values (here low simply to demonstrate the partitioning on disk)
	private static int MAX_NUM_NODE = 10; // Max number of nodes per file
	private static int MAX_REL = 10; // Max number of relationships per file

	// Hide constructor
	private Files() {
	}

	/**
	 * Based on node UUID, return the name of the node file (hash function)
	 * 
	 * @param nodeUUID
	 * @return
	 */
	protected static String getNodeFile(Long nodeUUID) {
		int numFile = 1 + (int) (nodeUUID / MAX_NUM_NODE);
		return "file-" + numFile;
	}

	/**
	 * Given the source Node UUID, return the name of the relationship file (hash
	 * function)
	 * 
	 * @param srcUUID
	 * @return
	 */
	protected static String getRelationFile(Long srcUUID) {
		int numFile = 1 + (int) (srcUUID / MAX_REL);
		return "file-" + numFile;
	}
}
