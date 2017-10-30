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
 * Describes a source file that should be compiled.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public interface SourceFile {
	File getSourceFolder();
	String getSourceFileName();
	File getSourceFile();
	File getDestinationFolder();
	String getEncoding();
}
