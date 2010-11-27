/**
 * Copyright © 2010 Reinier Zwitserloot.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class EnsureVersion extends Task {
	@Getter @Setter private String version;
	@Getter @Setter private String property;
	
	@Override public void execute() throws BuildException {
		if (version == null || version.isEmpty()) throw new BuildException("Must specify mandatory attribute 'version'", getLocation());
		
		List<VersionPart> want = toVersion(version);
		List<VersionPart> have = toVersion(Version.getVersion());
		if (!isEqualOrHigher(have, want)) {
			if (property == null) throw new BuildException("ivyplusplus version not sufficient; delete ivyplusplus.jar and run this script again. You have: " + Version.getVersion() + " but need: " + version);
		} else {
			if (property != null) getProject().setProperty(property, "true");
		}
	}
	
	public static List<VersionPart> toVersion(String version) {
		String[] parts = version.split("[-.]");
		List<VersionPart> ret = new ArrayList<VersionPart>();
		for (String part : parts) {
			ret.add(VersionPart.of(part));
		}
		return Collections.unmodifiableList(ret);
	}
	
	/**
	 * Returns {@code true} if {@code have} is a version equal to or higher than {@code want}.
	 */
	public static boolean isEqualOrHigher(List<VersionPart> have, List<VersionPart> want) {
		int largest = Math.max(have.size(), want.size());
		
		for (int i = 0; i < largest; i++) {
			VersionPart a = i < have.size() ? have.get(i) : VersionPart.zero();
			VersionPart b = i < want.size() ? want.get(i) : VersionPart.zero();
			int res = a.compareTo(b);
			if (res < 0) return false;
			if (res > 0) return true;
		}
		
		return true;
	}
	
	@Data
	private static class VersionPart implements Comparable<VersionPart> {
		private final int number;
		private final String name;
		
		public int compareTo(VersionPart o) {
			if (o.name == null) {
				if (name != null) return -1;
				if (number < o.number) return -1;
				if (number > o.number) return +1;
				return 0;
			}
			
			if (name == null) return +1;
			return name.compareTo(o.name);
		}
		
		public static VersionPart zero() {
			return new VersionPart(0, null);
		}
		
		public static VersionPart of(String part) {
			int v = 0;
			String name = null;
			for (char c : part.toCharArray()) {
				if (c >= '0' && c <= '9') {
					v = (v * 10) + (c - '0');
				} else {
					v = 0;
					name = part.trim();
					break;
				}
			}
			
			if (name != null && name.isEmpty()) name = null;
			if (name == null) return new VersionPart(v, null);
			else return new VersionPart(0, name);
		}
	}
}
