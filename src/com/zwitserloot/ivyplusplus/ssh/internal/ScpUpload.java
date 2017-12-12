package com.zwitserloot.ivyplusplus.ssh.internal;

import java.io.File;
import java.io.IOException;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;

public class ScpUpload {
	public static void main(String[] args) {
		File from = new File(args[0]);
		String to = args[1];
		String server = args[2];
		int port = Integer.parseInt(args[3]);
		String username = args[4];
		File keyFile = new File(args[5]);
		File knownHosts = new File(args[6]);
		
		try {
			execute(from, to, server, port, username, keyFile, knownHosts);
		} catch (IOException e) {
			System.out.println("T: " + e.getMessage());
			System.exit(1);
		}
	}
	
	public static void execute(File from, String to, String server, int port, String username, File keyFile, final File knownHosts) throws IOException {
		SSHClient ssh = new SSHClient();
		SshUtil.handleKnownHosts(ssh, knownHosts);
		System.out.println("V:connecting to " + username + "@" + server + ":" + port + " with key file " + keyFile.getAbsolutePath());
		ssh.connect(server, port);
		if (!SshUtil.handleKeyFile(ssh, username, keyFile)) {
			System.exit(0);
			return;
		}
		ssh.newSCPFileTransfer().upload(new FileSystemFile(from), to);
		System.exit(0);
	}
}
