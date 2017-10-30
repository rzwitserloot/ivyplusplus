package com.zwitserloot.ivyplusplus.ecj;

import java.io.File;

/**
 * Describes a source file that should be compiled.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class SourceFileImpl implements SourceFile {
	private static final String FILE_ENCODING_SYSTEM_PROPERTY = "file.encoding";
	private File _sourceFolder;
	private String _sourceFileName;
	private File _destinationFolder;
	private String _encoding;
	
	public SourceFileImpl(File sourceFolder, String sourceFileName, File destinationFolder, String encoding) {
		Assure.isDirectory("sourceFolder", sourceFolder);
		Assure.nonEmpty("sourceFileName", sourceFileName);
		Assure.isDirectory("destinationFolder", destinationFolder);
		Assure.nonEmpty("encoding", encoding);
		this._destinationFolder = destinationFolder;
		this._encoding = encoding;
		this._sourceFileName = sourceFileName;
		this._sourceFolder = sourceFolder;
	}
	
	public SourceFileImpl(File sourceFolder, String sourceFileName, File destinationFolder) {
		this(sourceFolder, sourceFileName, destinationFolder, System.getProperty(FILE_ENCODING_SYSTEM_PROPERTY));
	}
	
	protected SourceFileImpl(File sourceFolder, String sourceFileName) {
		Assure.isDirectory("sourceFolder", sourceFolder);
		Assure.nonEmpty("sourceFileName", sourceFileName);
		this._sourceFileName = sourceFileName;
		this._sourceFolder = sourceFolder;
		this._encoding = System.getProperty(FILE_ENCODING_SYSTEM_PROPERTY);
	}
	
	public File getSourceFolder() {
		return this._sourceFolder;
	}
	
	public String getSourceFileName() {
		return this._sourceFileName;
	}
	
	public File getSourceFile() {
		return new File(this._sourceFolder, this._sourceFileName);
	}
	
	public File getDestinationFolder() {
		return this._destinationFolder;
	}
	
	public boolean hasDestinationFolder() {
		return this._destinationFolder != null;
	}
	
	public String getEncoding() {
		return this._encoding;
	}
	
	@Override public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[SourceFile:");
		buffer.append(" _sourceFolder: ");
		buffer.append(this._sourceFolder);
		buffer.append(" _sourceFileName: ");
		buffer.append(this._sourceFileName);
		buffer.append(" _destinationFolder: ");
		buffer.append(this._destinationFolder);
		buffer.append(" _encoding: ");
		buffer.append(this._encoding);
		buffer.append("]");
		return buffer.toString();
	}
}
