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

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * @author Gerd Wuetherich (gerd@gerd-wuetherich.de)
 */
public class FileClassFileImpl extends DefaultReferableType implements ClassFile {
	private File _classfile;
	
	public FileClassFileImpl(File classfile, String libraryLocation, byte libraryType) {
		super(libraryLocation, libraryType);
		Assure.exists("classfile", classfile);
		this._classfile = classfile;
	}
	
	public byte[] getBytes() {
		try {
			return Util.getFileByteContent(this._classfile);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public final IBinaryType getBinaryType() {
		try {
			return ClassFileReader.read(this._classfile, true);
		} catch (Exception e) {
			// return null if an exception occurs
			e.printStackTrace();
			return null;
		}
	}
	
	@Override public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[FileClassFileImpl:");
		buffer.append(" bundleLocation: ");
		buffer.append(getLibraryLocation());
		buffer.append(" bundleType: ");
		buffer.append(getLibraryType());
		buffer.append(" accessRestriction: ");
		buffer.append(getAccessRestriction());
		buffer.append(" classfile: ");
		buffer.append(this._classfile);
		buffer.append("]");
		return buffer.toString();
	}
}
