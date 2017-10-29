/**
 * Copyright © 2011-2017 Reinier Zwitserloot.
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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator;
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

public class CreateSigningKey_ {
	public void createSigningKey(String identity, String passphrase, PrintStream log) throws IOException, SigningException {
		if (passphrase == null) passphrase = "";
		SecureRandom sr = new SecureRandom();
		
		try {
			DSAKeyPairGenerator gen = new DSAKeyPairGenerator();
			gen.init(new KeyGenerationParameters(sr, 1024));
			log.printf("Please hold on, a 1024 bit key is being generated. This takes a while and requires a lot of random data, so try moving the mouse at random.\n");
			
			AsymmetricCipherKeyPair privPair = gen.generateKeyPair();
			ElGamalKeyPairGenerator elgamal = new ElGamalKeyPairGenerator();
			BigInteger g = new BigInteger("153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc", 16);
			BigInteger p = new BigInteger("9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b", 16);
			elgamal.init(new ElGamalKeyGenerationParameters(sr, new ElGamalParameters(p, g)));
			
			AsymmetricCipherKeyPair signPair = elgamal.generateKeyPair();
			
			OutputStream privOut = new FileOutputStream("mavenrepo-signing-key-secret.bpr");
			try {
				OutputStream pubOut = new FileOutputStream("mavenrepo-signing-key-public.bpr");
				try {
					export(privOut,pubOut, privPair, signPair, identity, passphrase);
				} finally {
					pubOut.close();
				}
			} finally {
				privOut.close();
			}
		} catch (NoSuchProviderException e) {
			throw new SigningException("Bouncycastle provider not loaded", e);
		} catch (PGPException e) {
			e.printStackTrace();
			throw new SigningException("Unknown signing problem: " + e.getMessage(), e);
		}
	}
	
	void export(OutputStream privOut, OutputStream pubOut, AsymmetricCipherKeyPair privPair_, AsymmetricCipherKeyPair signPair_, String identity, String passphrase) throws PGPException, NoSuchProviderException, IOException {
		PGPKeyPair privPair = new BcPGPKeyPair(PGPPublicKey.DSA, privPair_, new Date());
		PGPKeyPair signPair = new BcPGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, signPair_, new Date());
		
		PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
		PGPKeyRingGenerator ringGen = new PGPKeyRingGenerator(
				PGPSignature.POSITIVE_CERTIFICATION,
				privPair,
				identity,
				sha1Calc, null, null,
				new JcaPGPContentSignerBuilder(privPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1),
				new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc).setProvider("BC").build(passphrase.toCharArray()));
		ringGen.addSubKey(signPair);
		ringGen.generateSecretKeyRing().encode(privOut);
		privOut.close();
		
		ringGen.generatePublicKeyRing().encode(pubOut);
		pubOut.close();
	}
}
