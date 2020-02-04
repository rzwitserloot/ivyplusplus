package com.zwitserloot.ivyplusplus.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarUnpacker {
	private static final Pattern classSpec = Pattern.compile("^.*com/zwitserloot/ivyplusplus/ssh/internal/.*\\.class$");
	
	private static String getPackHash() throws IOException {
		InputStream ph = JarUnpacker.class.getResourceAsStream("/packhash");
		if (ph == null) throw new IOException("Missing packhash");
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(ph, "UTF-8"));
			return br.readLine();
		} finally {
			try {
				ph.close();
			} catch (Exception ignore) {}
		}
	}
	
	public static void unpack(File tgtDir, String hashGuard) throws IOException {
		String homeOfClass = findClasspathRoot(JarUnpacker.class);
		String packHash = getPackHash();
		if (packHash.equals(hashGuard)) return;
		
		try (JarFile jf = new JarFile(homeOfClass)) {
			new File(tgtDir, "HASH").delete();
			deleteAll(tgtDir, 0);
			
			Enumeration<JarEntry> en = jf.entries();
			if (hashGuard != null) {
			}
			while (en.hasMoreElements()) {
				JarEntry entry = en.nextElement();
				if (entry.getName().endsWith(".jar")) {
					File out = new File(tgtDir, "lib");
					out.mkdirs();
					out = new File(out, entry.getName());
					InputStream in = jf.getInputStream(entry);
					transfer(in, out);
				} else {
					Matcher m = classSpec.matcher(entry.getName());
					if (!m.matches()) continue;
					File out = new File(tgtDir, "classes");
					out.mkdirs();
					String[] parts = entry.getName().split("/");
					for (String part : parts) {
						out.mkdirs();
						out = new File(out, part);
					}
					InputStream in = jf.getInputStream(entry);
					transfer(in, out);
				}
			}
		}
		
		File f = new File(tgtDir, "HASH");
		OutputStream hashOut = new FileOutputStream(f);
		try {
			byte[] phb = packHash.getBytes("UTF-8");
			byte[] phb2 = new byte[phb.length + 1];
			System.arraycopy(phb, 0, phb2, 0, phb.length);
			phb2[phb.length] = '\n';
			hashOut.write(phb2);
		} finally {
			hashOut.close();
		}
	}
	
	private static void deleteAll(File tgtDir, int lp) {
		if (lp > 50) return;
		for (File f : tgtDir.listFiles()) {
			if (f.isFile()) f.delete();
			if (f.isDirectory()) deleteAll(f, lp+1);
		}
		if (lp > 0) tgtDir.delete();
	}
	
	private static void transfer(InputStream in, File out) throws IOException {
		OutputStream stream = null;
		try {
			byte[] b = new byte[65536];
			stream = new FileOutputStream(out);
			while (true) {
				int r = in.read(b);
				if (r == -1) return;
				stream.write(b, 0, r);
			}
		} finally {
			try {
				in.close();
			} catch (Throwable ignore) {}
			if (stream != null) stream.close();
		}
	}
	
	private static String urlDecode(String in) {
		try {
			return URLDecoder.decode(in, Charset.defaultCharset().name());
		} catch (UnsupportedEncodingException e) {
			try {
				return URLDecoder.decode(in, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				return in;
			}
		}
	}
	
	private static String findClasspathRoot(Class<?> context) {
		URL selfURL = context.getResource(context.getSimpleName() + ".class");
		String self = selfURL.toString();
		if (self.startsWith("file:/")) {
			self = urlDecode(self.substring(5));
			Package p = context.getPackage();
			String pName = p == null ? "" : (p.getName().replace(".", "/") + "/");
			String suffix = "/" + pName + context.getSimpleName() + ".class";
			if (self.endsWith(suffix)) self = self.substring(0, self.length() - suffix.length());
			else throw new IllegalArgumentException("Unknown path structure: " + self);
		} else if (self.startsWith("jar:")) {
			int sep = self.indexOf('!');
			if (sep == -1) throw new IllegalArgumentException("No separator in jar protocol: " + self);
			String jarLoc = self.substring(4, sep);
			if (jarLoc.startsWith("file:/")) {
				jarLoc = urlDecode(jarLoc.substring(5));
				self = jarLoc;
			} else throw new IllegalArgumentException("Unknown path structure: " + self);
		} else {
			throw new IllegalArgumentException("Unknown protocol: " + self);
		}
		if (self.endsWith("/bin")) self = self.substring(0, self.length() - 4);
		else if (self.endsWith("/build")) self = self.substring(0, self.length() - 6);
		
		if (self.isEmpty()) self = "/";
		
		if (self.length() >= 3 && self.charAt(0) == '/' && self.charAt(2) == ':') {
			// Handling for windows paths including drive letter.
			self = self.substring(1);
		}
		
		return self;
	}
}
