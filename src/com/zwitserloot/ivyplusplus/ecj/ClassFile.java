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
import org.eclipse.jdt.internal.compiler.env.IBinaryType;

/**
 * An instance of class {@link ClassFile} represents a java class file that is
 * requested during the compilation process. A {@link ClassFile} is associated
 * with a {@link IBinaryType} and may has an {@link AccessRestriction}.
 * 
 * @author Gerd Wuetherich (gerd@gerd-wuetherich.de)
 * @author Nils Hartmann (nils@nilshartmann.net)
 */
public interface ClassFile extends ReferableType {
	/**
	 * Returns the class file as an {@link IBinaryType}.
	 * 
	 * @return this class file as an {@link IBinaryType}.
	 */
	IBinaryType getBinaryType();
	
	byte[] getBytes();
}