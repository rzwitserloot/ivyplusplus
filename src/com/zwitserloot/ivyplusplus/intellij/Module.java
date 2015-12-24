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
package com.zwitserloot.ivyplusplus.intellij;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;

public class Module {
	private List<Srcdir> srcdirs = new ArrayList<Srcdir>();
	private String name;
	private String depends;
	
	List<Srcdir> getSrcdirs() {
		return srcdirs;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDepends() {
		return depends;
	}
	
	public void setDepends(String depends) {
		this.depends = depends;
	}
	
	public void addSrcdir(Srcdir srcdir) {
		srcdirs.add(srcdir);
	}
	
	void validate(Location location) {
		for (Srcdir dir : srcdirs) {
			if (dir.getDir() == null) throw new BuildException("<srcdir> requires a 'src' attribute with the source dir you'd like to include.", location);
		}
		if (name == null || name.isEmpty()) throw new BuildException("<module> requires a 'name' attribute.", location);
	}
}
