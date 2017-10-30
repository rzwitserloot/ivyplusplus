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
import java.util.Arrays;

/**
 * Provides static factory methods to create class file finders.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class ClassFileLoaderFactory {
	public static ClassFileLoader createClasspathClassFileLoader(File source, byte type, File[] classpathEntries, File[] sourcepathEntries) {
		ClassFileLoaderCacheKey cacheKey = new ClassFileLoaderCacheKey(source, type, classpathEntries, sourcepathEntries);
		
		// Try to get already initialized ClassFileLoader from cache
		ClassFileLoader classFileLoader = ClassFileLoaderCache.getInstance().getClassFileLoader(cacheKey);
		if (classFileLoader == null) {
			classFileLoader = new ClasspathClassFileLoaderImpl(source, type, classpathEntries, sourcepathEntries);
			ClassFileLoaderCache.getInstance().storeClassFileLoader(cacheKey, classFileLoader);
		}
		return classFileLoader;
	}
	
	private static class ClassFileLoaderCacheKey {
		private final File _source;
		private final byte _type;
		private final File[] _classpathEntries;
		private final File[] _sourcepathEntries;
		
		public ClassFileLoaderCacheKey(File source, byte type, File[] classpathEntries, File[] sourcepathEntries) {
			this._source = source;
			this._type = type;
			this._classpathEntries = classpathEntries;
			this._sourcepathEntries = sourcepathEntries;
		}
		
		@Override public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(this._classpathEntries);
			result = prime * result + ((this._source == null) ? 0 : this._source.hashCode());
			result = prime * result + Arrays.hashCode(this._sourcepathEntries);
			result = prime * result + this._type;
			return result;
		}
		
		@Override public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			ClassFileLoaderCacheKey other = (ClassFileLoaderCacheKey) obj;
			if (!Arrays.equals(this._classpathEntries, other._classpathEntries)) return false;
			if (this._source == null) {
				if (other._source != null) return false;
			} else if (!this._source.equals(other._source)) return false;
			if (!Arrays.equals(this._sourcepathEntries, other._sourcepathEntries)) return false;
			if (this._type != other._type) return false;
			return true;
		}
		
		@Override public String toString() {
			return "ClassFileLoaderCacheKey [_source=" + this._source + ", _type=" + this._type + ", _classpathEntries="
					+ Arrays.toString(this._classpathEntries) + ", _sourcepathEntries="
					+ Arrays.toString(this._sourcepathEntries) + "]";
		}
	}

	/**
	 * Creates an new instance of type {@link ClassFileLoader}, that can load {@link ClassFile ClassFiles} from a jar file
	 * or directory.
	 * 
	 * @param entry The class path entry for the {@link ClassFileLoader}.
	 * @param type The type of the source. Possible values are {@link EcjAdapter#LIBRARY} and {@link EcjAdapter#PROJECT}.
	 * @return a new instance of type {@link ClassFileLoader}.
	 */
	public static ClassFileLoader createClasspathClassFileLoader(File entry, byte type) {
		String cacheKey = String.valueOf(entry) + "/" + type;
		ClassFileLoader classFileLoader = ClassFileLoaderCache.getInstance().getClassFileLoader(cacheKey);
		if (classFileLoader == null) {
			classFileLoader = new ClasspathClassFileLoaderImpl(entry, type);
			ClassFileLoaderCache.getInstance().storeClassFileLoader(cacheKey, classFileLoader);
		}
		return classFileLoader;
	}
	
	/**
	 * Creates an new instance of type {@link ClassFileLoader}, that can load classes from multiple underlying class file
	 * loaders.
	 * 
	 * @param classFileLoaders The class file loaders that should be contained in the compound class file loader.
	 * @return an new instance of type {@link ClassFileLoader}, that can load classes from multiple underlying class file loaders.
	 */
	public static ClassFileLoader createCompoundClassFileLoader(ClassFileLoader[] classFileLoaders) {
		return new CompoundClassFileLoaderImpl(classFileLoaders);
	}
	
	/**
	 * Creates an new instance of type {@link ClassFileLoader}, that can filter the access to classes in an underlying
	 * class file loader.
	 * 
	 * @param classFileLoader The underlying class file loader.
	 * @param filter The filter.
	 * @return The class file loader.
	 */
	public static ClassFileLoader createFilteringClassFileLoader(ClassFileLoader classFileLoader, String filter) {
		return new FilteringClassFileLoader(classFileLoader, filter);
	}
}
