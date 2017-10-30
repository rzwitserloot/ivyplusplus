package com.zwitserloot.ivyplusplus.ecj;

import java.io.File;

/**
 * Factory to create {@link SourceFile SourceFiles}.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class SourceFileFactory {
	public static SourceFile createSourceFile(File sourceFolder, String sourceFileName, File destinationFolder, String encoding) {
		return new SourceFileImpl(sourceFolder, sourceFileName, destinationFolder, encoding);
	}
	
	public static SourceFile createSourceFile(File sourceFolder, String sourceFileName, File destinationFolder) {
		return new SourceFileImpl(sourceFolder, sourceFileName, destinationFolder);
	}
}
