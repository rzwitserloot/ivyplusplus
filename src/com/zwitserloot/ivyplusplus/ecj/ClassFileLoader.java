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

/**
 * {@link ClassFileLoader} instances are used to load {@link ClassFile
 * ClassFiles} that are required during the compilation process.
 * 
 * @author Nils Hartmann (nils@nilshartmann.net)
 * @author Gerd Wuetherich (gerd@gerd-wuetherich.de)
 */
public interface ClassFileLoader {
	/**
	 * Returns all packages that can be loaded from this
	 * {@link ClassFileLoader}. The result contains visible packages as well as
	 * packages with access restrictions.
	 * 
	 * @return all packages that can be loaded from this {@link ClassFileLoader}.
	 */
	String[] getAllPackages();
	
	/**
	 * This method returns {@code true} if {@link ClassFileLoader} has a
	 * package with the name (regardless of any visibility restrictions).
	 * 
	 * @return {@code true} if the package is available via this {@link ClassFileLoader}.
	 */
	boolean hasPackage(String packageName);
	
	/**
	 * Returns an instance of type {@link ClassFile} that represents the
	 * specified class or <code>null</code> if no such class can be found.
	 * 
	 * @param className The class name of the class that should be loaded.
	 * @return An instance of type {@link ClassFile} that represents the
	 *         specified class or <code>null</code> if the class is not
	 *         available.
	 */
	ClassFile loadClass(ClassName className);
	
	/**
	 * Returns an instance of type {@link SourceFile} that is the source file
	 * for the specified class or {@code null} if no such source can be
	 * found.
	 * 
	 * @param className The class name of the class that should be loaded.
	 * @return An instance of type {@link SourceFile} that is the source file
	 *         for the specified class or {@code null} if no such source
	 *         can be found.
	 */
	ReferableSourceFile loadSource(ClassName className);
	
	/**
	 * Returns a list of File entries describing classpath entries associated
	 * with this ClassFileLoader instance. Please note that the list may not be
	 * complete if the implementation of this interface provides a class from an
	 * unknown source. Each returned File instance has to be canonical.
	 * 
	 * @return A list of File entries constituting the classpath. Not {@code null}.
	 */
	File[] getClasspath();
	
}