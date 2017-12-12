package com.zwitserloot.ivyplusplus.ssh.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;

import com.hierynomus.sshj.userauth.keyprovider.OpenSSHKeyV1KeyFile;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyFormat;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil;
import net.schmizz.sshj.userauth.keyprovider.OpenSSHKeyFile;
import net.schmizz.sshj.userauth.keyprovider.PKCS5KeyFile;
import net.schmizz.sshj.userauth.keyprovider.PKCS8KeyFile;
import net.schmizz.sshj.userauth.keyprovider.PuTTYKeyFile;

class SshUtil {
	static class KnownHost {
		String hostName;
		int port;
		String algorithm;
		String format;
		String base64;
	}
	
	static List<KnownHost> readKnownHosts(File f) throws IOException {
		if (!f.exists()) return Collections.emptyList();
		InputStream raw = new FileInputStream(f);
		List<KnownHost> out = new ArrayList<>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(raw, "UTF-8"));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) continue;
				String[] p = line.split(":", 5);
				KnownHost kn = new KnownHost();
				kn.hostName = p[0];
				kn.port = Integer.parseInt(p[1]);
				kn.algorithm = p[2];
				kn.format = p[3];
				kn.base64 = p[4];
				out.add(kn);
			}
		} finally {
			raw.close();
		}
		
		return out;
	}

	public static void handleKnownHosts(SSHClient ssh, final File knownHosts) throws IOException {
		final List<KnownHost> knownHostsList = readKnownHosts(knownHosts);
		ssh.addHostKeyVerifier(new HostKeyVerifier() {
			@Override public boolean verify(String hostName, int port, PublicKey key) {
				String b64 = Base64.toBase64String(key.getEncoded());
				for (KnownHost kn : knownHostsList) {
					if (kn.hostName.equals(hostName) && kn.port == port && kn.algorithm.equals(key.getAlgorithm()) && kn.format.equals(key.getFormat()) && kn.base64.equals(b64)) {
						System.out.println("V:host signature matched in known hosts file " + knownHosts.getAbsolutePath());
						return true;
					}
				}
				System.out.println("T:Known hosts file does not list this server. Edit " + knownHosts.getAbsolutePath() + " and add this line:");
				System.out.println("T:" + hostName + ":" + port + ":" + key.getAlgorithm() + ":" + key.getFormat() + ":" + b64);
				return false;
			}
		});
	}
	
	public static boolean handleKeyFile(SSHClient ssh, String username, File keyFile) throws IOException {
		KeyFormat format = KeyProviderUtil.detectKeyFileFormat(keyFile);
		KeyProvider kp;
		
		switch (format) {
		default:
		case Unknown:
			System.out.println("T:Not a recognized key file format: " + keyFile + " (" + format + ")");
			return false;
		case OpenSSHv1:
			kp = new OpenSSHKeyV1KeyFile();
			((OpenSSHKeyV1KeyFile) kp).init(keyFile);
			break;
		case OpenSSH:
			kp = new OpenSSHKeyFile();
			((OpenSSHKeyFile) kp).init(keyFile);
			break;
		case PuTTY:
			kp = new PuTTYKeyFile();
			((PuTTYKeyFile) kp).init(keyFile);
			break;
		case PKCS8:
			kp = new PKCS8KeyFile();
			((PKCS8KeyFile) kp).init(keyFile);
			break;
		case PKCS5:
			kp = new PKCS5KeyFile();
			((PKCS5KeyFile) kp).init(keyFile);
			break;
		}
		
		ssh.authPublickey(username, kp);
		return true;
	}
}
