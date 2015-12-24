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
package com.zwitserloot.ivyplusplus.mavencentral;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class CreateArtifactBundle extends Task {
	private File src, bin, javadoc, pom, key, out;
	private String version, artifactId, passphrase;
	private boolean noSourceOrJavadocNeeded;
	
	public void setSrc(File src) {
		this.src = src;
	}
	
	public void setBin(File bin) {
		this.bin = bin;
	}
	
	public void setJavadoc(File javadoc) {
		this.javadoc = javadoc;
	}
	
	public void setPom(File pom) {
		this.pom = pom;
	}
	
	public void setKey(File key) {
		this.key = key;
	}
	
	public void setOut(File out) {
		this.out = out;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	
	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}
	
	public void setNoSourceOrJavadocNeeded(boolean noSourceOrJavadocNeeded) {
		this.noSourceOrJavadocNeeded = noSourceOrJavadocNeeded;
	}
	
	@Override public void execute() throws BuildException {
		try {
			InitializeBouncyCastle.init();
		} catch (SigningException e) {
			throw new BuildException(e.getMessage(), e.getCause(), getLocation());
		}
		if (bin == null) throw new BuildException("Must specify mandatory attribute 'bin' (points at the jar with class files in it)", getLocation());
		if (pom == null) throw new BuildException("Must specify mandatory attribute 'pom' (points at your maven pom file)", getLocation());
		if (out == null) throw new BuildException("Must specify mandatory attribute 'out' (this is where the artifact bundle will be written to)", getLocation());
		if (key == null) throw new BuildException("Must specify mandatory attribute 'key' (file containing your signing key. Created by running ivyplusplus.jar with java -jar)", getLocation());
		if (version == null) throw new BuildException("Must specify mandatory attribute 'version'", getLocation());
		if (artifactId == null) throw new BuildException("Must specify mandatory attribute 'artifactId'", getLocation());
		if (passphrase == null) passphrase = "";
		
		if (!noSourceOrJavadocNeeded && src == null) throw new BuildException("Must specify mandatory attribute 'src' unless 'noSourceOrJavadocNeeded' is true (points at the jar with sources in it)", getLocation());
		if (!noSourceOrJavadocNeeded && javadoc == null) throw new BuildException("Must specify mandatory attribute 'javadoc' unless 'noSourceOrJavadocNeeded' is true (points at the jar with javadocs in it)", getLocation());
		
		if (bin != null && !bin.getName().toLowerCase().endsWith(".jar")) throw new BuildException("Only jar files are supported by create-artifact-bundle; 'bin' attribute isn't one.", getLocation());
		if (src != null && !src.getName().toLowerCase().endsWith(".jar")) throw new BuildException("Only jar files are supported by create-artifact-bundle; 'src' attribute isn't one.", getLocation());
		if (javadoc != null && !javadoc.getName().toLowerCase().endsWith(".jar")) throw new BuildException("Only jar files are supported by create-artifact-bundle; 'javadoc' attribute isn't one.", getLocation());
		
		byte[] pomData;
		try {
			InputStream pomStream = new FileInputStream(pom);
			try {
				pomData = readStream(pomStream);
			} finally {
				pomStream.close();
			}
		} catch (FileNotFoundException e) {
			throw new BuildException("Missing pom file", e, getLocation());
		} catch (IOException e) {
			throw new BuildException("Can't read pom file", e, getLocation());
		}
		
		if (version != null && !version.isEmpty()) {
			pomData = replaceVersion(pomData, "@VERSION@", version);
		}
		
		CreateDetachedSignatures signer = new CreateDetachedSignatures();
		
		try {
			FileOutputStream outStream = new FileOutputStream(out);
			try {
				JarOutputStream zipOut = new JarOutputStream(outStream);
				zipOut.putNextEntry(new JarEntry("pom.xml"));
				zipOut.write(pomData);
				ByteArrayOutputStream signature = new ByteArrayOutputStream();
				signer.signFile(new ByteArrayInputStream(pomData), signature, key, passphrase);
				zipOut.putNextEntry(new JarEntry("pom.xml.asc"));
				zipOut.write(signature.toByteArray());
				
				writeToJar(zipOut, artifactId + "-" + version + ".jar", bin);
				if (src != null) writeToJar(zipOut, artifactId + "-" + version + "-sources.jar", src);
				if (src != null) writeToJar(zipOut, artifactId + "-" + version + "-javadoc.jar", javadoc);
				zipOut.closeEntry();
				zipOut.close();
			} finally {
				outStream.close();
			}
		} catch (FileNotFoundException e) {
			throw new BuildException("File not found: " + e.getMessage(), e, getLocation());
		} catch (IOException e) {
			throw new BuildException("I/O problem writing out file or reading in files", e, getLocation());
		} catch (SigningException e) {
			throw new BuildException("Problem signing files", e, getLocation());
		}
	}
	
	private void writeToJar(JarOutputStream zipOut, String filename, File data) throws IOException, SigningException {
		ByteArrayOutputStream signature = new ByteArrayOutputStream();
		zipOut.putNextEntry(new JarEntry(filename));
		FileInputStream fis = new FileInputStream(data);
		try {
			CreateDetachedSignatures signer = new CreateDetachedSignatures();
			signer.signFile(new DuplicatingInputStream(fis, zipOut), signature, key, passphrase);
			zipOut.putNextEntry(new JarEntry(filename + ".asc"));
			zipOut.write(signature.toByteArray());
			zipOut.closeEntry();
		} finally {
			fis.close();
		}
	}
	
	private static class DuplicatingInputStream extends InputStream {
		private final InputStream wrapped;
		private final OutputStream pipe;
		
		DuplicatingInputStream(InputStream wrapped, OutputStream pipe) {
			this.wrapped = wrapped;
			this.pipe = pipe;
		}
		
		@Override public int read() throws IOException {
			int c = wrapped.read();
			if (c != -1) pipe.write(c);
			return c;
		}
		
		@Override public int read(byte[] b) throws IOException {
			int r = wrapped.read(b);
			if (r != -1) pipe.write(b, 0, r);
			return r;
		}
		
		@Override public int read(byte[] b, int off, int len) throws IOException {
			int r = wrapped.read(b, off, len);
			if (r != -1) pipe.write(b, off, r);
			return r;
		}
	}
	
	private static byte[] readStream(InputStream in) throws IOException {
		byte[] b = new byte[4096];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		while (true) {
			int r = in.read(b);
			if (r == -1) return out.toByteArray();
			out.write(b, 0, r);
		}
	}
	
	private static byte[] replaceVersion(byte[] in, String token_, String replacement_) {
		try {
			int start = 0;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] token = token_.getBytes("US-ASCII");
			byte[] replacement = replacement_.getBytes("US-ASCII");
			
			while (true) {
				int idx = find(in, token, start);
				if (idx == -1) break;
				out.write(in, start, idx - start);
				start = idx + token.length;
				out.write(replacement);
			}
			
			if (start < in.length) {
				out.write(in, start, in.length - start);
			}
			
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("IOException on mem-only operations", e);
		}
	}
	
	private static int find(byte[] haystack, byte[] needle, int start) {
		int pos = 0;
		for (int i = start; i < haystack.length; i++) {
			if (pos == needle.length) return i;
			if (haystack[i] == needle[pos]) pos++;
			else pos = 0;
		}
		
		return -1;
	}
}
