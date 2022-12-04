package dev.kooqix.graphxql;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;

import com.esotericsoftware.minlog.Log;

import dev.kooqix.exceptions.NodeTypeExistsException;

public class Hdfs {
	private static Configuration conf = new Configuration();

	private static FileSystem fs;

	private static boolean init = false;

	// Hide implicit public constructor
	private Hdfs() {
	}

	private static void init() throws IOException {
		if (!init) {
			fs = FileSystem.get(conf);
			init = true;
		}
	}

	private static void close() throws IOException {
		if (init) {
			fs.close();
			init = false;
		}
	}

	/**
	 * Rename a file
	 * 
	 * @param input
	 * @param output
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static boolean renameTo(String input, String output) throws IllegalArgumentException, IOException {
		init();
		boolean res = fs.rename(new Path(input), new Path(output));
		close();
		return res;
	}

	/**
	 * Get the content of a file (as string)
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static String readFile(String filename) throws IOException {
		init();
		InputStream content = null;
		String fileContent = null;

		try {
			content = fs.open(new Path(filename));
			fileContent = org.apache.commons.io.IOUtils.toString(content, "UTF-8");
		} finally {
			if (content != null)
				content.close();
		}
		close();
		return fileContent;
	}

	/**
	 * Whether a file already exists or not
	 * 
	 * @param url
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static boolean fileExists(String filename) throws IllegalArgumentException, IOException {
		init();
		return fs.exists(new Path(filename));
	}

	/**
	 * Create new directory
	 * 
	 * @param directoryName
	 * @param replace       Whether to replace the existing one (if already one)
	 * @throws IOException
	 * @throws NodeTypeExistsException
	 */
	public static void createDirectory(String directoryName, boolean replace)
			throws IOException, NodeTypeExistsException {
		init();
		Path path = new Path(directoryName);

		if (!fileExists(directoryName))
			fs.mkdirs(path);
		else {
			if (replace)
				fs.mkdirs(path);
			else
				throw new NodeTypeExistsException(directoryName);
		}
		close();
	}

	/**
	 * Write to file (overwrite)
	 * 
	 * @param filename The file to write to (path+filename)
	 * @param content  The content to write
	 * @throws IOException
	 */
	public static void writeFile(String filename, String content) throws IOException {
		init();
		Path path = new Path(filename);

		FSDataOutputStream fsDataOutputStream = fs.create(path);
		try (BufferedWriter bufferedWriter = new BufferedWriter(
				new OutputStreamWriter(fsDataOutputStream, StandardCharsets.UTF_8))) {
			bufferedWriter.write(content);
			bufferedWriter.newLine();
		}
		close();
	}

	/**
	 * Append to file
	 * 
	 * @param filename The file to append to (path+filename)
	 * @param content  Content to append
	 * @throws IOException
	 */
	public static void appendToFile(String filename, String content) throws IOException {
		init();
		Path path = new Path(filename);
		FSDataOutputStream fsDataOutputStream = fs.append(path);
		try (BufferedWriter bufferedWriter = new BufferedWriter(
				new OutputStreamWriter(fsDataOutputStream, StandardCharsets.UTF_8))) {
			bufferedWriter.write(content);
			bufferedWriter.newLine();
		}
		close();
	}

	/**
	 * Copy file
	 * 
	 * @param srcFilename
	 * @param destFilename
	 * @throws IOException
	 */
	public static void copyFile(String srcFilename, String destFilename) throws IOException {
		init();
		FileUtil.copy(fs, new Path(srcFilename), fs, new Path(destFilename), false, conf);

		close();
	}

	/**
	 * Delete file / directory
	 * 
	 * @param filename
	 * @param recursive
	 * @throws IOException
	 */
	public static void delete(String filename, boolean recursive) throws IOException {
		init();
		Path file = new Path(filename);
		fs.delete(file, recursive);
		close();
	}

	/**
	 * Delete all files and directories (recursively), without deleting the given
	 * directory
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void deleteUnder(String directory) throws IOException {
		Path path;

		List<String> files = listFiles(directory);

		Log.info("\n\n\n" + directory + "\n" + files.toString());

		init();
		for (String file : files) {
			path = new Path(file);
			fs.delete(path, true);
		}
		close();
	}

	/**
	 * Move file
	 * 
	 * @param srcFilename
	 * @param destFilename
	 * @throws IOException
	 */
	public static void move(String srcFilename, String destFilename) throws IOException {
		init();
		FileUtil.copy(fs, new Path(srcFilename), fs, new Path(destFilename), true, conf);

		close();
	}

	/**
	 * Get all files from a directory
	 * 
	 * @param directory
	 * @return
	 * @throws IOException
	 */
	public static List<String> listFiles(String directory) throws IOException {
		init();
		RemoteIterator<LocatedFileStatus> files = fs.listFiles(new Path(directory), false);

		List<String> listFiles = new ArrayList<>();
		LocatedFileStatus file;

		while (files.hasNext()) {
			file = files.next();

			listFiles.add(file.getPath().getName());
		}
		close();
		return listFiles;
	}

	/**
	 * Get all directories from a directory
	 * 
	 * @param directory
	 * @return
	 * @throws IOException
	 */
	public static List<String> listDirectories(String directory) throws IOException {
		init();
		FileStatus[] files = fs.listStatus(new Path(directory));

		List<String> listDirectories = new ArrayList<>();
		for (FileStatus fileStatus : files) {
			if (fileStatus.isDirectory())
				listDirectories.add(fileStatus.getPath().getName());

		}
		close();
		return listDirectories;
	}

}
