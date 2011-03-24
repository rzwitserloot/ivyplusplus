package com.zwitserloot.ivyplusplus.mavencentral;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class CreateDetachedSignatures {
	public void signFile(File file, File keyFile, String passphrase) throws IOException, SigningException {
		InitializeBouncyCastle.call(getClass().getName() + "_", "signFile",
				Arrays.<Class<?>>asList(File.class, File.class, String.class),
				Arrays.<Object>asList(file, keyFile, passphrase));
	}
	
	public void signFile(InputStream dataIn, OutputStream signOut, File keyFile, String passphrase) throws SigningException, IOException {
		InitializeBouncyCastle.call(getClass().getName() + "_", "signFile",
				Arrays.<Class<?>>asList(InputStream.class, OutputStream.class, File.class, String.class),
				Arrays.<Object>asList(dataIn, signOut, keyFile, passphrase));
	}
}
