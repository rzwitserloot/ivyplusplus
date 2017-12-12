package com.zwitserloot.ivyplusplus.ssh.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

public class SshExec {
	public static void main(String[] args) {
		String cmd = args[0];
		String server = args[1];
		int port = Integer.parseInt(args[2]);
		String username = args[3];
		File keyFile = new File(args[4]);
		File knownHosts = new File(args[5]);
		
		try {
			execute(cmd, server, port, username, keyFile, knownHosts);
		} catch (IOException e) {
			System.out.println("T: " + e.getMessage());
			System.exit(1);
		}
	}
	
	public static void execute(String cmd, String server, int port, String username, File keyFile, final File knownHosts) throws IOException {
		SSHClient ssh = new SSHClient();
		SshUtil.handleKnownHosts(ssh, knownHosts);
		ssh.connect(server, port);
		if (!SshUtil.handleKeyFile(ssh, username, keyFile)) {
			System.exit(0);
			return;
		}
		Session session = ssh.startSession();
		System.out.println("V:Starting session to run cmd \"" + cmd + "\"");
		Command command = session.exec(cmd);
		InputStream in = command.getInputStream();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				System.out.println("C:" + line);
			}
		} finally {
			in.close();
		}
		command.join(5, TimeUnit.SECONDS);
		Integer i = command.getExitStatus();
		if (i == null) System.exit(0);
		if (i.intValue() != 0) System.out.println("T:Remote process exited with error code " + i.intValue());
		System.exit(0);
	}
}
