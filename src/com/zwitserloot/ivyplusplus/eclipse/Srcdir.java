/**
 * Copyright © 2010-2020 Reinier Zwitserloot.
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
package com.zwitserloot.ivyplusplus.eclipse;

import java.io.File;

public class Srcdir {
	private File dir;
	private boolean optional = false;
	private boolean test = false;
	private String srcout = "";
	
	public File getDir() {
		return dir;
	}
	
	public void setDir(File dir) {
		this.dir = dir;
	}
	
	public String getSrcout() {
		return srcout;
	}
	
	public void setSrcout(String srcout) {
		this.srcout = srcout;
	}
	
	public boolean isOptional() {
		return optional;
	}
	
	public void setOptional(boolean optional) {
		this.optional = optional;
	}
	
	public boolean isTest() {
		return test;
	}
	
	public void setTest(boolean test) {
		this.test = test;
	}
	
	@Override public String toString() {
		String out = "Srcdir [dir=" + dir + ", optional=" + optional + ", test=" + test;
		if (!srcout.isEmpty()) out += ", srcout=" + srcout;
		return out + "]";
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dir == null) ? 0 : dir.hashCode());
		result = prime * result + (optional ? 1231 : 1237);
		result = prime * result + (test ? 1231 : 1237);
		result = prime * result + srcout.hashCode();
		return result;
	}
	
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Srcdir other = (Srcdir) obj;
		if (dir == null) {
			if (other.dir != null) return false;
		} else if (!dir.equals(other.dir)) return false;
		if (optional != other.optional) return false;
		if (test != other.test) return false;
		if (!srcout.equals(other.srcout)) return false;
		return true;
	}
}
