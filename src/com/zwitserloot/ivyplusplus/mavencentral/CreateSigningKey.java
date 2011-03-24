package com.zwitserloot.ivyplusplus.mavencentral;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

public class CreateSigningKey {
	public void createSigningKey(String identity, String passphrase, PrintStream log) throws IOException, SigningException {
		InitializeBouncyCastle.call(getClass().getName() + "_", "createSigningKey",
				Arrays.<Class<?>>asList(String.class, String.class, PrintStream.class),
				Arrays.<Object>asList(identity, passphrase, log));
	}
}
