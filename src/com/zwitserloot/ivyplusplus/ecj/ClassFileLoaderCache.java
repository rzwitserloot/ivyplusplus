package com.zwitserloot.ivyplusplus.ecj;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 * @author Nils Hartmann
 */
public class ClassFileLoaderCache {
	private static final boolean ENABLE_CACHE = Boolean.getBoolean("ant4eclipse.enableClassFileLoaderCache");
	private Map<Object, ClassFileLoader> _classFileLoaderMap;
	@SuppressWarnings("unused") private int _hits = 0;
	@SuppressWarnings("unused") private int _missed = 0;
	private boolean _initialized;
	public ClassFileLoaderCache() {
		this._classFileLoaderMap = new ConcurrentHashMap<Object, ClassFileLoader>();
	}
	
	public boolean isInitialized() {
		return this._initialized;
	}
	
	public void initialize() {
		this._initialized = true;
	}
	
	public void dispose() {
		this._initialized = false;
	}
	
	public void clear() {
		this._classFileLoaderMap.clear();
		this._hits = 0;
		this._missed = 0;
	}
	
	public void storeClassFileLoader(Object key, ClassFileLoader classFileLoader) {
		if (ENABLE_CACHE) this._classFileLoaderMap.put(key, classFileLoader);
	}
	public ClassFileLoader getClassFileLoader(Object key) {
		ClassFileLoader classFileLoader = this._classFileLoaderMap.get(key);
		if (classFileLoader != null) {
			this._hits++;
		} else {
			this._missed++;
		}
		return classFileLoader;
	}
	
	public boolean hasClassFileLoader(Object key) {
		return this._classFileLoaderMap.containsKey(key);
	}
	
	private static final ClassFileLoaderCache INSTANCE = new ClassFileLoaderCache();
	public static ClassFileLoaderCache getInstance() {
		return INSTANCE;
	}
}
