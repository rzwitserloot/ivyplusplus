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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

public class CreateDetachedSignatures_ {
	public void signFile(File file, File keyFile, String passphrase) throws IOException, SigningException {
		try {
			PGPSecretKey key;
			{
				FileInputStream keyIn = new FileInputStream(keyFile);
				try {
					key = getSigningKey(keyIn, keyFile.getName());
				} finally {
					keyIn.close();
				}
			}
			
			{
				OutputStream outStream = new FileOutputStream(file.getAbsolutePath() + ".asc");
				try {
					InputStream fileIn = new FileInputStream(file);
					try {
						signFile(fileIn, key, passphrase, outStream);
					} finally {
						fileIn.close();
					}
				} finally {
					outStream.close();
				}
			}
		} catch (NoSuchProviderException e) {
			throw new SigningException("Bouncycastle provider not loaded", e);
		} catch (NoSuchAlgorithmException e) {
			throw new SigningException("Signature key uses an algorithm that is not compatible with ivyplusplus", e);
		} catch (PGPException e) {
			throw new SigningException("Unknown signing problem: " + e.getMessage(), e);
		} catch (SignatureException e) {
			throw new SigningException("Problem with signature: " + e.getMessage(), e);
		}
	}
	
	public void signFile(InputStream dataIn, OutputStream signOut, File keyFile, String passphrase) throws IOException, SigningException {
		try {
			PGPSecretKey key;
			{
				FileInputStream keyIn = new FileInputStream(keyFile);
				try {
					key = getSigningKey(keyIn, keyFile.getName());
				} finally {
					keyIn.close();
				}
			}
			
			{
				signFile(dataIn, key, passphrase, signOut);
			}
		} catch (NoSuchProviderException e) {
			throw new SigningException("Bouncycastle provider not loaded", e);
		} catch (NoSuchAlgorithmException e) {
			throw new SigningException("Signature key uses an algorithm that is not compatible with ivyplusplus", e);
		} catch (PGPException e) {
			throw new SigningException("Unknown signing problem: " + e.getMessage(), e);
		} catch (SignatureException e) {
			throw new SigningException("Problem with signature: " + e.getMessage(), e);
		}
	}
	
	void signFile(InputStream fileData, PGPSecretKey signingKey, String passphrase, OutputStream out) throws IOException, NoSuchProviderException, PGPException, NoSuchAlgorithmException, SignatureException {
		PGPDigestCalculatorProvider provider = new BcPGPDigestCalculatorProvider();
		PBESecretKeyDecryptor decryptor = new BcPBESecretKeyDecryptorBuilder(provider).build(passphrase.toCharArray());
		PGPPrivateKey privKey = signingKey.extractPrivateKey(decryptor);
		PGPSignatureGenerator sigGen = new PGPSignatureGenerator(
			new BcPGPContentSignerBuilder(signingKey.getPublicKey().getAlgorithm(), PGPUtil.SHA1));
		sigGen.init(PGPSignature.BINARY_DOCUMENT, privKey);
		out = new ArmoredOutputStream(out);
		BCPGOutputStream bOut = new BCPGOutputStream(out);
		byte[] b = new byte[4096];
		while (true) {
			int r = fileData.read(b);
			if (r == -1) break;
			sigGen.update(b, 0, r);
		}
		
		sigGen.generate().encode(bOut);
		bOut.close();
		out.close();
	}
	
	PGPSecretKey getSigningKey(InputStream keyData, String streamName) throws IOException, PGPException, SigningException {
		PGPSecretKeyRingCollection keyrings_ = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyData), new BcKeyFingerprintCalculator());
		Iterator<?> keyrings = keyrings_.getKeyRings();
		while (keyrings.hasNext()) {
			PGPSecretKeyRing keys_ = (PGPSecretKeyRing) keyrings.next();
			Iterator<?> keys = keys_.getSecretKeys();
			while (keys.hasNext()) {
				PGPSecretKey key = (PGPSecretKey) keys.next();
				if (key.isSigningKey()) return key;
			}
		}
		
		throw new SigningException("No signing key found in keyring: " + streamName);
	}
}
