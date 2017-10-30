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

import org.eclipse.jdt.internal.compiler.env.AccessRestriction;

/**
 * A {@link ReferableType} is a class file or a source file that can be referred
 * from a project that should be build.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public interface ReferableType {
	/**
	 * Returns the type of the bundle this {@link ReferableType} was loaded
	 * from. Possible values are {@link EcjAdapter#LIBRARY} and
	 * {@link EcjAdapter#PROJECT}.
	 * 
	 * @return the type of the bundle this class file was loaded from.
	 */
	byte getLibraryType();
	
	/**
	 * Returns of location of the bundle this {@link ReferableType} was loaded
	 * from.
	 * 
	 * @return the location of the bundle this {@link ReferableType} was loaded from.
	 */
	String getLibraryLocation();
	
	/**
	 * Returns whether there exists an access restriction for this class file or
	 * not.
	 * 
	 * @return whether there exists an access restriction for this class file or not.
	 */
	boolean hasAccessRestriction();
	
	/**
	 * Returns the access restriction for this class file or {@code null}
	 * if no access restriction exists.
	 * 
	 * @return the access restriction for this class file or {@code null}
	 *         if no access restriction exists.
	 */
	AccessRestriction getAccessRestriction();
}