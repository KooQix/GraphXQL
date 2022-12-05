package dev.kooqix.graphxql;

public class Files {
	// private static int MAX_NUM_NODE = 500;
	// private static int MAX_REL = 1000;
	private static int MAX_NUM_NODE = 10;
	private static int MAX_REL = 10;

	private Files() {
	}

	protected static String getNodeFile(Long nodeUUID) {
		int numFile = 1 + (int) (nodeUUID / MAX_NUM_NODE);
		return "file-" + numFile;
	}

	protected static String getRelationFile(Long srcUUID) {
		int numFile = 1 + (int) (srcUUID / MAX_REL);
		return "file-" + numFile;
	}
}
