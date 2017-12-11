/**
 * Copyright © 2017 Reinier Zwitserloot.
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

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class Hardlink extends Task {
	private File from;
	private File to;
	
	public void setFrom(File from) {
		this.from = from;
	}
	
	public void setTo(File to) {
		this.to = to;
	}
	
	public void execute() throws BuildException {
		if (from == null) throw new BuildException("Specify 'from'");
		if (to == null) throw new BuildException("Specify 'to'");
		
		File lnExe = new File("/bin/ln");
		if (lnExe.isFile()) executePosix();
		else executeWindows();
	}
	
	private void executeWindows() {
		if (to.isFile()) to.delete();
		String w = System.getenv().get("windir");
		if (w == null || w.isEmpty()) w = "C:\\Windows";
		ProcessBuilder pb = new ProcessBuilder(w + "\\System32\\fsutil.exe", "hardlink", "create", to.getAbsolutePath(), from.getAbsolutePath());
		int errCode;
		try {
			errCode = pb.start().waitFor();
		} catch (InterruptedException e) {
			throw new BuildException("interrupted");
		} catch (IOException e) {
			throw new BuildException(e);
		}
		if (0 != errCode) throw new BuildException("Hardlinking failed: " + errCode);
	}
	
	private void executePosix() {
		ProcessBuilder pb = new ProcessBuilder("/bin/ln", "-f", from.getAbsolutePath(), to.getAbsolutePath());
		int errCode;
		try {
			errCode = pb.start().waitFor();
		} catch (InterruptedException e) {
			throw new BuildException("interrupted");
		} catch (IOException e) {
			throw new BuildException(e);
		}
		if (0 != errCode) throw new BuildException("Hardlinking failed: " + errCode);
	}
}
