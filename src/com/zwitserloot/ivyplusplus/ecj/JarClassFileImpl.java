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

import java.io.IOException;
import java.util.zip.ZipFile;

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * @author Gerd Wuetherich (gerd@gerd-wuetherich.de)
 */
public class JarClassFileImpl extends DefaultReferableType implements ClassFile {
	private ZipFile _zipFile;
	private String _zipEntryName;
	
	public JarClassFileImpl(String zipEntryName, ZipFile zipFile, String libraryLocation, byte libraryType) {
		super(libraryLocation, libraryType);
		
		Assure.nonEmpty("zipEntryName", zipEntryName);
		Assure.notNull("zipFile", zipFile);
		
		this._zipEntryName = zipEntryName;
		this._zipFile = zipFile;
	}
	
	public byte[] getBytes() {
		try {
			return Util.getZipEntryByteContent(this._zipFile.getEntry(this._zipEntryName), this._zipFile);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public final IBinaryType getBinaryType() {
		try {
			return ClassFileReader.read(this._zipFile, this._zipEntryName, true);
		} catch (ClassFormatException e) {
			throw new RuntimeException("Can't read binary " + this._zipEntryName + " from jar " + this._zipFile, e);
		} catch (IOException e) {
			throw new RuntimeException("Can't read binary " + this._zipEntryName + " from jar " + this._zipFile, e);
		} catch (java.lang.SecurityException e) {
			throw new RuntimeException("Can't read binary " + this._zipEntryName + " from jar " + this._zipFile, e);
		}
	}
	
	@Override public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[JarClassFileImpl:");
		buffer.append(" bundleLocation: ");
		buffer.append(getLibraryLocation());
		buffer.append(" bundleType: ");
		buffer.append(getLibraryType());
		buffer.append(" accessRestriction: ");
		buffer.append(getAccessRestriction());
		buffer.append(" zipFile: ");
		buffer.append(this._zipFile);
		buffer.append(" zipEntryName: ");
		buffer.append(this._zipEntryName);
		buffer.append("]");
		return buffer.toString();
	}
}