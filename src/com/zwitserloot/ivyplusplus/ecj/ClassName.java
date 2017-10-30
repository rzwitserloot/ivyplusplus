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

/**
 * Represents a <b>qualified</b> class name.
 * 
 * @author Nils Hartmann (nils@nilshartmann.net)
 */
public final class ClassName {
	private String _packageName;
	
	private String _className;
	
	private String _qualifiedName;
	
	private ClassName(String qualifiedClassName) {
		this._qualifiedName = qualifiedClassName;
		this._packageName = "";
		this._className = qualifiedClassName;
		
		int v = qualifiedClassName.lastIndexOf('.');
		if (v != -1) {
			this._packageName = qualifiedClassName.substring(0, v);
			this._className = qualifiedClassName.substring(v + 1);
		}
	}
	
	/**
	 * Returns the qualified name of this class as a java type identifier (e.g.
	 * {@code foo.bar.Bazz}).
	 * 
	 * @return The qualified class name. Neither {@code null} nor empty.
	 */
	public String getQualifiedClassName() {
		return this._qualifiedName;
	}
	
	/**
	 * Returns the name of this class without package (e.g. {@code Bazz}).
	 * 
	 * @return Name of this class. Never null.
	 */
	public String getClassName() {
		return this._className;
	}
	
	/**
	 * Returns the package name of this class (e.g. {@code foo.bar}).
	 * 
	 * @return Package name of this class. Never null.
	 */
	public String getPackageName() {
		return this._packageName;
	}
	
	/**
	 * Returns this package as a directory name (e.g. {@code foo/bar}).
	 * 
	 * @return this package as a directory name. Never null.
	 */
	public String getPackageAsDirectoryName() {
		return getPackageName().replace('.', '/');
	}
	
	/**
	 * Returns this class name as a classname including the package directory
	 * structure and the ".class" postfix. (e.g.
	 * {@code foo/bar/Bazz.class}).
	 * 
	 * @return this class name as a file name.
	 */
	public String asClassFileName() {
		String fileName = getQualifiedClassName().replace('.', '/');
		return fileName + ".class";
	}
	
	/**
	 * Returns this class name as a classname including the package directory
	 * structure and the ".java" ending (e.g. {@code foo/bar/Bazz.java}).
	 * 
	 * @return this class name as a file name.
	 */
	public String asSourceFileName() {
		String fileName = getQualifiedClassName().replace('.', '/');
		return fileName + ".java";
	}
	
	@Override public String toString() {
		return this._qualifiedName;
	}
	
	@Override public int hashCode() {
		return this._qualifiedName.hashCode();
	}
	
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ClassName other = (ClassName) obj;
		return this._qualifiedName.equals(other._qualifiedName);
	}
	
	/**
	 * Returns a new instance of type {@link ClassName} representing the given
	 * qualified class name.
	 * 
	 * @param qualifiedClassName The qualified class name
	 * @return a ClassName instance representing this qualified class name
	 */
	public static final ClassName fromQualifiedClassName(String qualifiedClassName) {
		Assure.nonEmpty("qualifiedClassName", qualifiedClassName);
		return new ClassName(qualifiedClassName);
	}
	
}