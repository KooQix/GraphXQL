package dev.kooqix.database;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import dev.kooqix.exceptions.InvalidSchemaFieldException;
import dev.kooqix.exceptions.JobFailedException;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class NodeType implements Serializable {
	private static Configuration conf = new Configuration();
	private static FileSystem fs;
	private static boolean init = false;

	private static void init() throws IOException {
		if (!init) {
			fs = FileSystem.get(conf);
			init = true;
		}
	}

	private static void closeFS() throws IOException {
		if (init) {
			fs.close();
			init = false;
		}
	}

	private String name;
	private Schema schema;
	private String filename;
	private DataFileWriter<GenericRecord> dataFileWriter;

	private String path;

	/**
	 * When loading
	 * 
	 * @param name
	 */
	protected NodeType(String name) {
		this.name = name.toLowerCase();
		this.filename = this.name + ".avro";
	}

	/**
	 * Create new nodetype
	 * 
	 * @param name
	 * @param schemaFile
	 * @throws IOException
	 * @throws InvalidSchemaFieldException
	 */
	public NodeType(String name, String schemaFile) throws IOException, InvalidSchemaFieldException {
		this.name = name.toLowerCase();

		this.filename = this.name + ".avro";

		// Load schema from given .avsc file
		try {
			this.schema = new Schema.Parser().parse(new File(schemaFile));
		} catch (Exception e) {
			throw new InvalidSchemaFieldException("Invalid file. Must be a valid .avsc file");
		}
		if (this.schema.getField("uuid") == null)
			throw new InvalidSchemaFieldException("uuid: long field is required");

		DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(this.schema);
		this.dataFileWriter = new DataFileWriter<>(datumWriter);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @throws JobFailedException
	 */
	public void setName(String name) throws JobFailedException {
		String oldName = this.name;

		// // Set directory name
		// File nodetypeFile = new File(MessageFormat.format("{0}/{1}.{2}", directory,
		// oldName, EXTENSION));

		// if (nodetypeFile.renameTo(new File(MessageFormat.format("{0}/{1}.{2}",
		// directory, name, EXTENSION))))
		// this.name = name;
		// else
		// throw new JobFailedException("Failed to rename");
	}

	/**
	 * @return the schema
	 */
	public Schema getSchema() {
		return schema;
	}

	public List<Field> getFields() {
		return this.schema.getFields();
	}

	/**
	 * @return the filename
	 */
	protected String getFilename() {
		return filename;
	}

	protected void setPath(String path) {
		this.path = path + "/" + this.filename;
	}

	protected void open() throws IOException {
		init();

		FSDataOutputStream fsDataOutputStream = fs.create(new Path(this.path));

		this.dataFileWriter.create(schema, fsDataOutputStream);
	}

	/**
	 * @return the dataFileWriter
	 */
	protected DataFileWriter<GenericRecord> getDataFileWriter() {
		return dataFileWriter;
	}

	protected void close() throws IOException {
		this.dataFileWriter.close();
		closeFS();
	}
}
