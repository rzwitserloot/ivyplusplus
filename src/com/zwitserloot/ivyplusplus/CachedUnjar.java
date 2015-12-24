/**
 * Copyright © 2011 Reinier Zwitserloot.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.zwitserloot.ivyplusplus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.taskdefs.Expand;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

public class CachedUnjar extends MatchingTask {
	private File dest;
	private File source;
	private File marker;
	private List<FileSet> fileSets = new ArrayList<FileSet>();
	private List<Path> paths = new ArrayList<Path>();
	
	public void addFileset(FileSet set) {
		this.fileSets.add(set);
	}
	
	public void addPath(Path path) {
		this.paths.add(path);
	}
	
	public void setSource(File file) {
		this.source = file;
	}
	
	public void setMarker(File file) {
		this.marker = file;
	}
	
	public void setDest(File file) {
		this.dest = file;
	}
	
	public void execute() throws BuildException {
		if (source == null && fileSets.isEmpty() && paths.isEmpty()) {
			throw new BuildException("Specify either 'source' or include a fileset or path.", getLocation());
		}
		
		if (marker == null) throw new BuildException("Specify 'marker' which is a file that carries caching info.", getLocation());
		if (dest == null) throw new BuildException("Specify 'dest' which is the directory the jars are unpacked to.", getLocation());
		
		if (!dest.exists()) if (!dest.mkdirs()) throw new BuildException("'dest' does not exist and cannot be created as directory: " + dest, getLocation());
		if (!dest.isDirectory()) throw new BuildException("'dest' must be a directory: " + dest, getLocation());
		
		if (source != null && (!fileSets.isEmpty() || !paths.isEmpty())) {
			throw new BuildException("Specify either 'source' or include filesets/paths, not both.", getLocation());
		}
		
		Set<CacheRecord> caches;
		try {
			caches = readCaches(marker);
		} catch (IOException e) {
			throw new BuildException("Can't read marker file", e, getLocation());
		}
		
		Set<CacheRecord> newCache = new LinkedHashSet<CacheRecord>();
		Set<CacheRecord> toUnpack = new LinkedHashSet<CacheRecord>();
		List<File> toUnpackRes = new ArrayList<File>();
		List<File> allRes = new ArrayList<File>();
		
		if (source != null) {
			FileSet fs = new FileSet();
			fs.setFile(source);
			fileSets.add(fs);
		}
		
		if (!fileSets.isEmpty()) {
			Path fsPath = new Path(getProject());
			for (FileSet fs : fileSets) fsPath.addFileset(fs);
			paths.add(fsPath);
		}
		
		try {
			for (Path path : paths) {
				Iterator<?> it = path.iterator();
				while (it.hasNext()) {
					Resource res = (Resource) it.next();
					if (!(res instanceof FileResource)) {
						throw new BuildException("Only file resources supported: " + res.getName(), getLocation());
					}
					File jarFile = ((FileResource)res).getFile();
					allRes.add(jarFile);
					CacheRecord cr = new CacheRecord(jarFile.getCanonicalPath(), res.getLastModified(), res.getSize());
					if (caches.contains(cr)) {
						this.log(String.format("Skipping %s due to cache", jarFile.getCanonicalPath()), Project.MSG_VERBOSE);
						newCache.add(cr);
					} else {
						newCache.add(cr);
						toUnpack.add(cr);
						toUnpackRes.add(jarFile);
					}
				}
			}
			
			if (newCache.size() - toUnpack.size() == caches.size()) {
				// We just need to unpack toUnpack.
				unpack(toUnpackRes);
			} else {
				// At least one unpack library is no longer in the set, so we delete and unpack all.
				this.log(String.format("Deleting %s because previously present jar is no longer there.", dest), Project.MSG_INFO);
				clearDest();
				unpack(allRes);
			}
		} catch (IOException e) {
			throw new BuildException(e, getLocation());
		}
		
		try {
			saveCache(newCache);
		} catch (IOException e) {
			throw new BuildException("Can't write marker file", e, getLocation());
		}
	}
	
	private void clearDest() {
		Delete d = new Delete();
		d.setDir(dest);
		d.execute();
	}
	
	private void unpack(Collection<File> ress) {
		for (File res : ress) {
			Expand ex = new Expand();
			ex.setDest(dest);
			ex.setSrc(res);
			ex.setTaskName("cachingunjar");
			ex.setTaskType("unjar");
			ex.execute();
		}
	}
	
	private void saveCache(Collection<CacheRecord> crs) throws IOException {
		FileOutputStream fos = new FileOutputStream(marker);
		try {
			for (CacheRecord cr : crs) fos.write(cr.write().getBytes("UTF-8"));
		} finally {
			fos.close();
		}
	}
	
	private static Set<CacheRecord> readCaches(File marker) throws IOException {
		Set<CacheRecord> out = new LinkedHashSet<CacheRecord>();
		try {
			FileInputStream fis = new FileInputStream(marker);
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					line = line.trim();
					if (line.startsWith("#")) continue;
					if (line.length() == 0) continue;
					out.add(CacheRecord.read(line));
				}
			} finally {
				fis.close();
			}
		} catch (FileNotFoundException e) {}
		return out;
	}
	
	private static class CacheRecord {
		private final String name;
		private final long lastMod, len;
		
		public CacheRecord(String name, long lastMod, long len) {
			this.name = name;
			this.lastMod = lastMod;
			this.len = len;
		}
		
		@Override public String toString() {
			return name + "[lastMod = " + lastMod + ", len = " + len + "]";
		}
		
		@Override public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (lastMod ^ (lastMod >>> 32));
			result = prime * result + (int) (len ^ (len >>> 32));
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		
		@Override public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			CacheRecord other = (CacheRecord) obj;
			if (lastMod != other.lastMod) return false;
			if (len != other.len) return false;
			if (name == null) {
				if (other.name != null) return false;
			} else if (!name.equals(other.name)) return false;
			return true;
		}
		
		static CacheRecord read(String line) {
			String[] elems = line.split(" ::: ", 3);
			return new CacheRecord(elems[0], Long.parseLong(elems[1]), Long.parseLong(elems[2]));
		}
		
		String write() {
			return String.format("%s ::: %d ::: %d\n", name, lastMod, len);
		}
	}
}
