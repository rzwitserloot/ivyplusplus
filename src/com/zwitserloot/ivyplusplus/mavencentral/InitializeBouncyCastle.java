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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Provider;
import java.security.Security;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Cleanup;
import lombok.Lombok;

public class InitializeBouncyCastle {
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	private static ClassLoader loader;
	
	public static void init() throws SigningException {
		if (initialized.getAndSet(true)) return;
		
		URL[] urls = new URL[3];
		urls[0] = writeToTemp("bcprov-jdk16");
		urls[1] = writeToTemp("bcpg-jdk16");
		urls[2] = writeToTemp("ipp-bc-bridges");
		
		try {
			loader = new URLClassLoader(urls, InitializeBouncyCastle.class.getClassLoader());
			Class<?> provider = loader.loadClass("org.bouncycastle.jce.provider.BouncyCastleProvider");
			Security.addProvider((Provider) provider.newInstance());
		} catch (ClassNotFoundException e) {
			throw new SigningException("Included bouncycastle provider jar is corrupted", e);
		} catch (IllegalAccessException e) {
			throw new SigningException("Included bouncycastle provider jar is corrupted", e);
		} catch (InstantiationException e) {
			throw new SigningException("Included bouncycastle provider jar is corrupted", e);
		}
	}
	
	private static URL writeToTemp(String resourceKey) throws SigningException {
		try {
			File file = File.createTempFile(resourceKey, "jar");
			file.deleteOnExit();
			
			@Cleanup FileOutputStream out = new FileOutputStream(file);
			@Cleanup InputStream in = InitializeBouncyCastle.class.getResourceAsStream("/" + resourceKey + ".jar");
			byte[] b = new byte[4096];
			while (true) {
				int r = in.read(b);
				if (r == -1) break;
				out.write(b, 0, r);
			}
			in.close();
			out.close();
			
			return file.toURI().toURL();
		} catch (IOException e) {
			throw new SigningException("Can't unpack bouncycastle crypto provider to temp dir: " + e, e);
		}
	}
	
	public static Object call(String fqn, String methodName, List<Class<?>> types, List<Object> params) throws SigningException {
		Throwable t;
		try {
			Class<?> c = loader.loadClass(fqn);
			return c.getMethod(methodName, types.toArray(new Class<?>[0])).invoke(c.newInstance(), params.toArray(new Object[0]));
		} catch (ClassNotFoundException e) {
			t = e;
		} catch (IllegalAccessException e) {
			t = e;
		} catch (InstantiationException e) {
			t = e;
		} catch (InvocationTargetException e) {
			throw Lombok.sneakyThrow(e);
		} catch (NoSuchMethodException e) {
			t = e;
		}
		
		t.printStackTrace();
		throw new SigningException("bouncycastle corrupted", t);
	}
}
