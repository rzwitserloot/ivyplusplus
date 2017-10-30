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

import java.util.Iterator;
import java.util.Map;

/**
 * Default implementation of a {@link CompileJobDescription}.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class CompileJobDescriptionImpl implements CompileJobDescription {
	private ClassFileLoader _classFileLoader;
	private Map<String, String> _compilerOptions;
	private SourceFile[] _sourceFiles;
	
	public CompileJobDescriptionImpl() {}
	
	public CompileJobDescriptionImpl(ClassFileLoader classFileLoader, Map<String, String> compilerOptions, SourceFile[] sourceFiles) {
		Assure.notNull("classFileLoader", classFileLoader);
		Assure.notNull("compilerOptions", compilerOptions);
		Assure.notNull("sourceFiles", sourceFiles);
		this._classFileLoader = classFileLoader;
		this._compilerOptions = compilerOptions;
		this._sourceFiles = sourceFiles;
	}
	
	public ClassFileLoader getClassFileLoader() {
		return this._classFileLoader;
	}
	
	public Map<String, String> getCompilerOptions() {
		return this._compilerOptions;
	}
	
	public SourceFile[] getSourceFiles() {
		return this._sourceFiles;
	}
	
	public void setClassFileLoader(ClassFileLoader classFileLoader) {
		Assure.notNull("classFileLoader", classFileLoader);
		this._classFileLoader = classFileLoader;
	}
	
	public void setCompilerOptions(Map<String, String> compilerOptions) {
		Assure.notNull("compilerOptions", compilerOptions);
		this._compilerOptions = compilerOptions;
	}
	
	public void setSourceFiles(SourceFile[] sourceFiles) {
		Assure.notNull("sourceFiles", sourceFiles);
		this._sourceFiles = sourceFiles;
	}
	
	@Override public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[DefaultCompileJobDescription:");
		buffer.append(" _classFileLoader: ");
		buffer.append(this._classFileLoader);
		buffer.append(", _compilerOptions: {");
		if ((this._compilerOptions != null) && (this._compilerOptions.size() > 0)) {
			Iterator<Map.Entry<String, String>> iterator = this._compilerOptions.entrySet().iterator();
			Map.Entry<String, String> current = iterator.next();
			buffer.append("(");
			buffer.append(current.getKey());
			buffer.append(",");
			buffer.append(current.getValue());
			buffer.append(")");
			while (iterator.hasNext()) {
				buffer.append(",");
				current = iterator.next();
				buffer.append("(");
				buffer.append(current.getKey());
				buffer.append(",");
				buffer.append(current.getValue());
				buffer.append(")");
			}
		}
		buffer.append(this._compilerOptions);
		buffer.append("}");
		buffer.append(", _sourceFiles: {");
		if ((this._sourceFiles != null) && (this._sourceFiles.length > 0)) {
			buffer.append(this._sourceFiles[0]);
			for (int i = 1; i < this._sourceFiles.length; i++) {
				buffer.append(", ");
				buffer.append(this._sourceFiles[i]);
			}
		}
		buffer.append(this._sourceFiles);
		buffer.append("}");
		buffer.append("]");
		return buffer.toString();
	}
	
}