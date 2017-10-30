package com.zwitserloot.ivyplusplus.ecj;

import org.eclipse.jdt.internal.compiler.env.AccessRestriction;

import java.io.File;

/**
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class ReferableSourceFileImpl extends SourceFileImpl implements ReferableSourceFile {
	private DefaultReferableType _referableType = new DefaultReferableType();
	
	public ReferableSourceFileImpl(File sourceFolder, String sourceFileName, String libraryLocation, byte libraryType) {
		super(sourceFolder, sourceFileName);
		Assure.notNull("libraryLocation", libraryLocation);
		this._referableType.setLibraryLocation(libraryLocation);
		this._referableType.setLibraryType(libraryType);
	}
	
	public final AccessRestriction getAccessRestriction() {
		return this._referableType.getAccessRestriction();
	}
	
	public String getLibraryLocation() {
		return this._referableType.getLibraryLocation();
	}
	
	public byte getLibraryType() {
		return this._referableType.getLibraryType();
	}
	
	public final boolean hasAccessRestriction() {
		return this._referableType.hasAccessRestriction();
	}
	
	public final void setAccessRestriction(AccessRestriction accessRestriction) {
		this._referableType.setAccessRestriction(accessRestriction);
	}
}