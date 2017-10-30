/**********************************************************************
 * Copyright (c) 2005-2008 ant4eclipse project team.
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
 * {@link DefaultReferableType} is the base class of all referable types.
 * 
 * @author Gerd Wuetherich (gerd@gerd-wuetherich.de)
 */
public class DefaultReferableType implements ReferableType {
	private String _libraryLocation;
	private byte _libraryType;
	private AccessRestriction _accessRestriction;
	
	public DefaultReferableType() {}
	
	protected DefaultReferableType(String libraryLocation, byte libraryType) {
		Assure.notNull("libraryLocation", libraryLocation);
		this._libraryLocation = libraryLocation;
		this._libraryType = libraryType;
	}
	
	public String getLibraryLocation() {
		return this._libraryLocation;
	}
	
	public byte getLibraryType() {
		return this._libraryType;
	}
	
	public final AccessRestriction getAccessRestriction() {
		return this._accessRestriction;
	}
	
	public final boolean hasAccessRestriction() {
		return this._accessRestriction != null;
	}
	
	public final void setAccessRestriction(AccessRestriction accessRestriction) {
		this._accessRestriction = accessRestriction;
	}
	
	public void setLibraryLocation(String libraryLocation) {
		this._libraryLocation = libraryLocation;
	}
	
	public void setLibraryType(byte libraryType) {
		this._libraryType = libraryType;
	}
}