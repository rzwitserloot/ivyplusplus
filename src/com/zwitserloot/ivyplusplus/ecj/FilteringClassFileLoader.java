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

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.env.AccessRestriction;
import org.eclipse.jdt.internal.compiler.env.AccessRule;

/**
 * @author Gerd Wuetherich (gerd@gerd-wuetherich.de)
 */
public class FilteringClassFileLoader implements ClassFileLoader {
	/** the class file loader that should be filtered */
	private ClassFileLoader _classFileLoader;
	private String _filter;
	private List<String> _includes;
	private List<String> _excludes;
	private Set<String> _containedPackages;
	
	public FilteringClassFileLoader(ClassFileLoader classFileLoader, String filter) {
		Assure.notNull("classFileLoader", classFileLoader);
		Assure.nonEmpty("filter", filter);
		
		this._classFileLoader = classFileLoader;
		this._filter = filter;
		
		this._includes = new LinkedList<String>();
		this._excludes = new LinkedList<String>();
		
		init();
	}
	
	public File[] getClasspath() {
		return this._classFileLoader.getClasspath();
	}
	
	public String[] getAllPackages() {
		return this._classFileLoader.getAllPackages();
	}
	
	public boolean hasPackage(String packageName) {
		return this._classFileLoader.hasPackage(packageName);
	}
	
	public ClassFile loadClass(ClassName className) {
		ClassFile result = this._classFileLoader.loadClass(className);
		setAccessRestrictions(result, className);
		return result;
	}
	
	public ReferableSourceFile loadSource(ClassName className) {
		ReferableSourceFile result = this._classFileLoader.loadSource(className);
		setAccessRestrictions(result, className);
		return result;
	}
	
	private ReferableType setAccessRestrictions(ReferableType referableType, ClassName className) {
		if (referableType == null) return referableType;
		
		// try 'shortcut'
		if (this._containedPackages != null && this._containedPackages.contains(className.getPackageName())) return referableType;
		
		String classFileName = className.asClassFileName();
		
		for (String includePattern : this._includes) {
			if (classFileName.matches(includePattern)) return referableType;
		}
		
		for (String exludePattern : this._excludes) {
			if (classFileName.matches(exludePattern)) {
				if (referableType instanceof DefaultReferableType) {
					AccessRestriction accessRestriction = new AccessRestriction(new AccessRule("**".toCharArray(), IProblem.ForbiddenReference), referableType.getLibraryType(), referableType.getLibraryLocation());
					((DefaultReferableType) referableType).setAccessRestriction(accessRestriction);
				}
				return referableType;
			}
		}
		
		return referableType;
	}
	
	private void init() {
		this._containedPackages = new HashSet<String>();
		
		for (String part : this._filter.split(";")) {
			// step 0:
			if (part.matches("\\+.*/\\*")) {
				this._containedPackages.add(part.substring(1, part.length() - 2).replace('/', '.'));
			} else if (part.matches("\\-\\*\\*/\\*")) {
				//
			} else {
				this._containedPackages = null;
			}
			
			// step 1: replace all occurrences of '**/*' with '###' (temporary step)
			String transformedPart = part.substring(1).replaceAll("\\*\\*/\\*", "###");
			
			// step 2: replace all occurrences of '*' with '[^\.]*'
			transformedPart = transformedPart.replaceAll("\\*", "[^\\\\.]*");
			
			// step 3: replace all occurrences of '###' (formally '**/*') with '.*'
			transformedPart = transformedPart.replaceAll("###", ".*");
			
			// step 4: append '\.class'
			transformedPart = transformedPart.concat("\\.class");
			
			if (part.startsWith("+")) this._includes.add(transformedPart);
			if (part.startsWith("-")) this._excludes.add(transformedPart);
		}
	}
}