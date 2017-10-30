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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Implementation of a class path based {@link ClassFileLoader}. An instance of this class contains an array of files
 * (jar files or directories) where the class file loader searches for classes.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class ClasspathClassFileLoaderImpl implements ClassFileLoader {
	private File[] _classpathEntries;
	private File[] _sourcepathEntries;
	private File _location;
	/** the type of the associated bundle (PROJECT or LIBRARY) */
	private byte _type;
	private Map<String, PackageProvider> _allPackages;

	public ClasspathClassFileLoaderImpl(File entry, byte type) {
		Assure.notNull("entry", entry);
		this._location = entry;
		this._type = type;
		initialize(new File[] { entry }, new File[] {});
	}
	
	public ClasspathClassFileLoaderImpl(File classPathEntry, byte type, File sourcePathEntry) {
		Assure.notNull("classPathEntry", classPathEntry);
		Assure.notNull("sourcePathEntry", sourcePathEntry);
		
		this._location = classPathEntry;
		this._type = type;
		
		initialize(new File[] { classPathEntry }, new File[] { sourcePathEntry });
	}
	
	public ClasspathClassFileLoaderImpl(File location, byte type, File[] classpathEntries) {
		Assure.notNull("location", location);
		
		this._location = location;
		this._type = type;
		
		initialize(classpathEntries, new File[] {});
	}
	
	public ClasspathClassFileLoaderImpl(File location, byte type, File[] classpathEntries, File[] sourcePathEntries) {
		Assure.notNull("location", location);
		
		this._location = location;
		this._type = type;
		
		initialize(classpathEntries, sourcePathEntries);
	}
	
	protected ClasspathClassFileLoaderImpl() {}
	
	public boolean hasPackage(String packageName) {
		return this._allPackages.containsKey(packageName);
	}
	
	public String[] getAllPackages() {
		Set<String> keys = this._allPackages.keySet();
		return keys.toArray(new String[0]);
	}

	protected void setLocation(File location) {
		this._location = location;
	}
	
	protected void setType(byte type) {
		this._type = type;
	}
	protected File getLocation() {
		return this._location;
	}
	protected byte getType() {
		return this._type;
	}
	
	protected File[] getClasspathEntries() {
		return this._classpathEntries;
	}
	
	public File[] getClasspath() {
		return getClasspathEntries();
	}
	
	protected void initialize(File[] classpathEntries, File[] sourcepathEntries) {
		Assure.notNull("classpathEntries", classpathEntries);
		Assure.notNull("sourcepathEntries", sourcepathEntries);
		
		for (File classpathEntrie : classpathEntries) Assure.notNull("classpathEntrie", classpathEntrie);
		for (File sourcepathEntry : sourcepathEntries) Assure.notNull("sourcepathEntry", sourcepathEntry);
		
		// assign path entries
		this._classpathEntries = classpathEntries;
		this._sourcepathEntries = sourcepathEntries;
		
		// create allPackages hash map
		this._allPackages = new HashMap<String, PackageProvider>();
		
		// add all existing packages to the hash map
		for (File file : this._classpathEntries) {
			if (file.isDirectory()) {
				String[] allPackages = getAllPackagesFromDirectory(file);
				addAllPackagesFromClassPathEntry(allPackages, file);
			} else if (file.isFile()) {
				String[] allPackages = getAllPackagesFromJar(file);
				addAllPackagesFromClassPathEntry(allPackages, file);
			}
		}
		
		// add all existing packages to the hash map
		for (File file : this._sourcepathEntries) {
			if (file.isDirectory()) {
				String[] allPackages = getAllPackagesFromDirectory(file);
				addAllPackagesFromSourcePathEntry(allPackages, file);
			} else {
				throw new RuntimeException("We do not support source in jars or zips");
			}
		}
	}
	
	/**
	 * Returns a new {@link PackageProvider}. This method returns a {@link PackageProvider} of type
	 * {@link PackageProvider}.
	 * 
	 * You can override this method to provide your own {@link PackageProvider} implementation.
   */
	protected PackageProvider newPackageProvider() {
		return new PackageProvider();
	}
	
	protected final PackageProvider getPackageProvider(String packageName) {
		return this._allPackages.get(packageName);
	}
	
	private void addAllPackagesFromClassPathEntry(String[] allPackages, File classPathEntry) {
		for (String aPackage : allPackages) {
			if (this._allPackages.containsKey(aPackage)) {
				PackageProvider provider = this._allPackages.get(aPackage);
				provider.addClasspathEntry(classPathEntry);
			} else {
				PackageProvider provider = newPackageProvider();
				provider.addClasspathEntry(classPathEntry);
				this._allPackages.put(aPackage, provider);
			}
		}
	}
	
	private void addAllPackagesFromSourcePathEntry(String[] allPackages, File sourcePathEntry) {
		for (String aPackage : allPackages) {
			if (this._allPackages.containsKey(aPackage)) {
				PackageProvider provider = this._allPackages.get(aPackage);
				provider.addSourcepathEntry(sourcePathEntry);
			} else {
				PackageProvider provider = newPackageProvider();
				provider.addSourcepathEntry(sourcePathEntry);
				this._allPackages.put(aPackage, provider);
			}
		}
	}
	
	/**
	 * Returns all the names of the packages that are contained in the specified jar file. The package list contains the
	 * packages that contain classes as well as all parent packages of those.
	 */
	private String[] getAllPackagesFromJar(File jar) {
		Assure.isFile("jar", jar);
		List<String> result = new ArrayList<String>();
		JarFile jarFile = null;
		
		try {
			jarFile = new JarFile(jar);
		} catch (IOException e) {
			throw new RuntimeException("Cannot load jar: " + jar.getAbsolutePath(), e);
		}
		
		Enumeration<?> enumeration = jarFile.entries();
		while (enumeration.hasMoreElements()) {
			JarEntry jarEntry = (JarEntry) enumeration.nextElement();
			
			// add package for each found directory...
			String directoryName = null;
			
			// if the jar entry is a directory, the directory name is the name of the jar entry...
			if (jarEntry.isDirectory()) {
				directoryName = jarEntry.getName();
			} else {
				// otherwise the directory name has to be computed
				int splitIndex = jarEntry.getName().lastIndexOf('/');
				if (splitIndex != -1) directoryName = jarEntry.getName().substring(0, splitIndex);
			}
			
			// directoryName can be null if a top level entry is processed
			if (directoryName != null) {
				// convert path to package name
				String packageName = directoryName.replace('/', '.');
				packageName = packageName.endsWith(".") ? packageName.substring(0, packageName.length() - 1) : packageName;
				
				// at package with all the parent packages (!) to the result list
				String[] packages = allPackages(packageName);
				for (int i = 0; i < packages.length; i++) {
					if (!result.contains(packages[i])) result.add(packages[i]);
				}
			}
		}
		return result.toArray(new String[0]);
	}
	
	/**
	 * Returns all package names (including parent package names) for the specified package.
	 * 
	 * <b>Example:</b><br />
	 * Given the package name {@code net.sf.ant4eclipse.tools} this method will return {"net", "net.sf",
	 * "net.sf.ant4eclipse", "net.sf.ant4eclipse.tools"}.
	 * 
	 * @param packageName The name of the package.
	 * @return all package names (including parent package names) for the specified package.
	 */
	private String[] allPackages(String packageName) {
		StringTokenizer tokenizer = new StringTokenizer(packageName, ".");
		String[] result = new String[tokenizer.countTokens()];
		
		for (int i = 0; i < result.length; i++) {
			result[i] = i == 0 ? tokenizer.nextToken() : result[i - 1] + "." + tokenizer.nextToken();
		}
		
		return result;
	}
	
	private String[] getAllPackagesFromDirectory(File directory) {
		List<String> result = new LinkedList<String>();
		File[] children = directory.listFiles(new FileFilter() {
			public boolean accept(File pathname) { return pathname.isDirectory(); }
		});
		
		if (children != null) {
			for (File element : children) getAllPackagesFromDirectory(null, element, result);
		}
		
		return result.toArray(new String[0]);
	}

	private void getAllPackagesFromDirectory(String prefix, File directory, List<String> result) {
		String newPrefix = prefix == null ? "" : prefix + ".";
		result.add(newPrefix + directory.getName());
		File[] children = directory.listFiles(new FileFilter() {
			public boolean accept(File pathname) {return pathname.isDirectory();}
		});
		
		if (children != null) {
			for (File element : children) getAllPackagesFromDirectory(newPrefix + directory.getName(), element, result);
		}
	}
	
	public ClassFile loadClass(ClassName className) {
		if (!hasPackage(className.getPackageName())) return null;
		return getPackageProvider(className.getPackageName()).loadClassFile(className);
	}
	
	public ReferableSourceFile loadSource(ClassName className) {
		if (!hasPackage(className.getPackageName())) return null;
		return getPackageProvider(className.getPackageName()).loadSourceFile(className);
	}
	
	@Override public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[ClasspathClassFileLoader:");
		buffer.append(" { ");
		for (int i0 = 0; (this._classpathEntries != null) && (i0 < this._classpathEntries.length); i0++) {
			buffer.append(" _classpathEntries[" + i0 + "]: ");
			buffer.append(this._classpathEntries[i0]);
		}
		buffer.append(" } ");
		buffer.append(" _location: ");
		buffer.append(this._location);
//		buffer.append(" _type: ");
//		buffer.append(this._type);
//		buffer.append(" _allPackages: ");
//		buffer.append(this._allPackages);
		buffer.append("]");
		return buffer.toString();
	}
	
	/**
	 * Encapsulates all class and source path entries that provide a specific package.
	 * 
	 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
	 */
	public class PackageProvider {
		private List<File> _classpathEntries;
		private List<File> _sourcepathEntries;
		
		public PackageProvider() {
			this._classpathEntries = new LinkedList<File>();
			this._sourcepathEntries = new LinkedList<File>();
		}
		
		public void addClasspathEntry(File classpathEntry) {
			Assure.exists("classpathEntry", classpathEntry);
			this._classpathEntries.add(classpathEntry);
		}
		
		public void addSourcepathEntry(File sourcepathEntry) {
			Assure.isDirectory("sourcepathEntry", sourcepathEntry);
			this._sourcepathEntries.add(sourcepathEntry);
		}
		
		public ClassFile loadClassFile(ClassName className) {
			for (File file : this._classpathEntries) {
				File classpathEntry = file;
				
				if (classpathEntry.isDirectory()) {
					File result = new File(classpathEntry, className.asClassFileName());
					if (result.exists()) {
						try {
							if (result.getName().equals(result.getCanonicalFile().getName())) {
								return new FileClassFileImpl(result, classpathEntry.getAbsolutePath(),
										ClasspathClassFileLoaderImpl.this._type);
							}
						} catch (IOException ignore) {}
					}
				} else {
					try {
						JarFile jarFile = new JarFile(classpathEntry);
						JarEntry entry = jarFile.getJarEntry(className.asClassFileName());
						if (entry != null) {
							return new JarClassFileImpl(className.asClassFileName(), jarFile, classpathEntry.getAbsolutePath(),
									ClasspathClassFileLoaderImpl.this._type);
						}
					} catch (IOException ignore) {}
				}
			}
			return null;
		}
		
		public ReferableSourceFile loadSourceFile(ClassName className) {
			String javaFileName = className.getClassName() + ".java";
			for (File classpathEntry : this._sourcepathEntries) {
				if (classpathEntry.isDirectory()) {
					File packageDir = new File(classpathEntry, className.getPackageAsDirectoryName());
					if (packageDir.isDirectory()) {
						for (String name : packageDir.list()) {
							if (javaFileName.equals(name)
									&& new File(classpathEntry, className.asSourceFileName().replace('/', File.separatorChar)
									.replace('\\', File.separatorChar)).exists()) {
								
								return new ReferableSourceFileImpl(classpathEntry, className.asSourceFileName()
										.replace('/', File.separatorChar).replace('\\', File.separatorChar),
										classpathEntry.getAbsolutePath(), ClasspathClassFileLoaderImpl.this._type);
							}
						}
					}
				} else {
					throw new RuntimeException("Source jars not supported.");
				}
			}
			return null;
		}
	}
}
