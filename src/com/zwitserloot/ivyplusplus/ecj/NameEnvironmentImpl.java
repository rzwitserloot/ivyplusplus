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

import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

public class NameEnvironmentImpl implements INameEnvironment {
	private final ClassFileLoader loader;
	
	public NameEnvironmentImpl(ClassFileLoader loader) {
		this.loader = loader;
	}
	
	@Override public void cleanup() {
		// Nothing to do
	}
	
	@Override public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
		return findClass(toJavaName(compoundTypeName));
	}
	
	@Override public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
		StringBuffer result = new StringBuffer();
		if (packageName != null) for (char[] elem : packageName) result.append(new String(elem)).append(".");
		return findClass(result.append(new String(typeName)).toString());
	}
	
	@Override public boolean isPackage(char[][] parentPackageName, char[] packageName) {
		String qualifiedPackageName = toJavaName(parentPackageName);
		if (qualifiedPackageName.length() > 0) {
			qualifiedPackageName += "." + new String(packageName);
		} else {
			qualifiedPackageName = new String(packageName);
		}
		
		return loader.hasPackage(qualifiedPackageName);
	}
	
	protected NameEnvironmentAnswer findClass(String className) {
		return findClass(ClassName.fromQualifiedClassName(className));
	}
	
	protected NameEnvironmentAnswer findClass(ClassName className) {
		ClassFile classFile = loader.loadClass(className);
		if (classFile != null) {
			return new NameEnvironmentAnswer(classFile.getBinaryType(), (classFile.hasAccessRestriction() ? classFile.getAccessRestriction() : null));
		}
		
		SourceFile sourceFile = loader.loadSource(className);
		if (sourceFile != null) {
			return new NameEnvironmentAnswer(new CompilationUnitImpl(sourceFile), null);
		}
		
		return null;
	}
	
	private static String toJavaName(char[][] array) {
		if (array == null) return "";
		StringBuffer result = new StringBuffer();
		boolean f = true;
		for (char[] a : array) {
			if (!f) result.append(".");
			result.append(new String(a));
			f = false;
		}
		return result.toString();
	}
}
