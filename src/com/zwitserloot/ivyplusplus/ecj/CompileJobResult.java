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

import java.io.File;
import java.util.Map;

/**
 * The {@link CompileJobResult} represents a compile job result.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public interface CompileJobResult {
	boolean succeeded();
	CategorizedProblem[] getCategorizedProblems();
	Map<String, File> getCompiledClassFiles();
}
