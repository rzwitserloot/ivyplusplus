/**********************************************************************
 * Copyright (c) 2005-2009 ant4eclipse project team.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nils Hartmann, Daniel Kasmeroglu, Gerd Wuetherich
 **********************************************************************/
package com.zwitserloot.ivyplusplus.ecj;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;

/**
 * <p>
 * Adapter class for providing java source files to the eclipse java compiler.
 * </p>
 * 
 * @author Nils Hartmann (nils@nilshartmann.net)
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class CompilationUnitImpl implements ICompilationUnit {
	private static final String JAVA_FILE_POSTFIX = ".java";
	private SourceFile _sourceFile;
	
	/** the file name, relative to the source folder */
	private char[] _fileName;
	
	/** the name of the top level public type, e.g. {Hashtable} */
	private char[] _mainTypeName;
	
	/** the name of the package , e.g. {java, lang} */
	private char[][] _packageName;
	
	public CompilationUnitImpl(SourceFile sourceFile) {
		Assure.notNull("sourceFile", sourceFile);
		this._sourceFile = sourceFile;
		this._fileName = this._sourceFile.getSourceFileName().toCharArray();
		
		// compute qualified name
		String qualifiedTypeName = getQualifiedTypeName(this._sourceFile.getSourceFileName());
		
		// compute package and main type name
		int v = qualifiedTypeName.lastIndexOf('.');
		this._mainTypeName = qualifiedTypeName.substring(v + 1).toCharArray();
		if ((v > 0) && (v < qualifiedTypeName.length())) {
			String packageName = qualifiedTypeName.substring(0, v);
			StringTokenizer packages = new StringTokenizer(packageName, ".");
			this._packageName = new char[packages.countTokens()][];
			for (int i = 0; i < this._packageName.length; i++) {
				this._packageName[i] = packages.nextToken().toCharArray();
			}
		} else {
			this._packageName = new char[0][];
		}
	}
	
	public final char[] getMainTypeName() {
		return this._mainTypeName;
	}
	
	public final char[][] getPackageName() {
		return this._packageName;
	}
	
	public final char[] getFileName() {
		return this._fileName;
	}
	
	public final char[] getContents() {
		String filename = new String(this._fileName);
		File sourceFile = new File(this._sourceFile.getSourceFolder(), filename);
		
		StringBuffer result = new StringBuffer();
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile), this._sourceFile.getEncoding()));
			
			String str;
			while ((str = in.readLine()) != null) {
				result.append(str);
				result.append("\n");
			}
		} catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Unable to read compilation content (encoding)", e);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read compilation content (I/O)", e);
		}
		return result.toString().toCharArray();
	}
	
	public SourceFile getSourceFile() {
		return this._sourceFile;
	}
	
	private String getQualifiedTypeName(String fileName) {
		if (fileName.toLowerCase().endsWith(JAVA_FILE_POSTFIX)) {
			return fileName.substring(0, fileName.length() - 5).replace(File.separatorChar, '.');
		} else {
			return fileName.replace(File.separatorChar, '.');
		}
	}
	
	public boolean ignoreOptionalProblems() {
		return false;
	}
}
