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

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implements the call-back interface {@link ICompilerRequestor} for receiving compilation results. The
 * {@link CompilerRequestorImpl} writes the compiled class files to disc or reports the errors in case the compilation
 * was not successful.
 * 
 * @author Nils Hartmann (nils@nilshartmann.net)
 */
public class CompilerRequestorImpl implements ICompilerRequestor {
	/** Indicates whether the compilation was successful or not. */
	protected boolean _compilationSuccessful;
	
	/** The list of categorized problems. */
	protected List<CategorizedProblem> _categorizedProblems;
	
	/** Collection of class files which have been compiled */
	private Map<String, File> _compiledClassFiles;

	public CompilerRequestorImpl() {
		this._compilationSuccessful = true;
		this._categorizedProblems = new LinkedList<CategorizedProblem>();
		this._compiledClassFiles = new Hashtable<String, File>();
	}
	
	public Map<String, File> getCompiledClassFiles() {
		return Collections.unmodifiableMap(this._compiledClassFiles);
	}
	
	public void acceptResult(CompilationResult result) {
		CompilationUnitImpl compilationUnitImpl = (CompilationUnitImpl) result.getCompilationUnit();
		SourceFile sourceFile = compilationUnitImpl.getSourceFile();
		File destinationDirectory = sourceFile.getDestinationFolder();
		
		this._compilationSuccessful = false;
		if (!result.hasErrors()) {
			this._compilationSuccessful = true;
			ClassFile[] classFiles = result.getClassFiles();
			for (ClassFile classFile2 : classFiles) {
				char[][] compoundName = classFile2.getCompoundName();
				StringBuffer classFileName = new StringBuffer();
				for (int j = 0; j < compoundName.length; j++) {
					classFileName.append(compoundName[j]);
					if (j < compoundName.length - 1) classFileName.append('/');
				}
				classFileName.append(".class");
				File classFile = new File(destinationDirectory, classFileName.toString());
				File classDir = classFile.getParentFile();
				if (!classDir.exists()) classDir.mkdirs();
				writeFile(classFile, classFile2.getBytes());
				this._compiledClassFiles.put(classFileName.toString(), classFile);
			}
		}
		
		if (result.getAllProblems() != null) this._categorizedProblems.addAll(Arrays.asList(result.getAllProblems()));
	}
	
	public boolean isCompilationSuccessful() {
		return this._compilationSuccessful;
	}
	
	public CategorizedProblem[] getCategorizedProblems() {
		return this._categorizedProblems.toArray(new CategorizedProblem[0]);
	}
	
	/**
	 * This function stores a file under a specified location using a chosen encoding.
	 * 
	 * @param destination The destination where the file has to be written to. Not {@code null}.
	 * @param content The content that has to be written. Not {@code null}.
	 */
	public static final void writeFile(File destination, byte[] content) {
		Assure.notNull("destination", destination);
		Assure.notNull("content", content);
		try (OutputStream output = new FileOutputStream(destination)) {
			output.write(content);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
