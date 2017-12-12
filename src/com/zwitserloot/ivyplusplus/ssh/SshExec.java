package com.zwitserloot.ivyplusplus.ssh;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

public class SshExec extends Task {
	private String cmd;
	private String server;
	private int port;
	private String username;
	private File keyFile;
	private File knownHosts;
	
	public void setCmd(String cmd) {
		this.cmd = cmd;
	}
	
	public void setServer(String server) {
		this.server = server;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setKeyFile(File keyFile) {
		this.keyFile = keyFile;
	}
	
	public void setKnownHosts(File knownHosts) {
		this.knownHosts = knownHosts;
	}
	
	@Override public void execute() throws BuildException {
		if (cmd == null) throw new BuildException("'cmd' is mandatory.");
		if (server == null) throw new BuildException("'server' is mandatory.");
		if (username == null) throw new BuildException("'username' is mandatory.");
		if (keyFile == null) throw new BuildException("'keyFile' is mandatory.");
		if (port == 0) port = 22;
		if (knownHosts == null) knownHosts = new File(getProject().getBaseDir(), "ssh.knownHosts");
		
		File loc = new File(getProject().getBaseDir(), "build/ssh");
		try {
			SshSubsystem.unpack(loc);
		} catch (IOException e) {
			log(e, Project.MSG_ERR);
			throw new BuildException("Can't unpack ssh subsystem into build/ssh");
		}
		SshSubsystem.call(loc, getProject(), this, "com.zwitserloot.ivyplusplus.ssh.internal.SshExec", cmd, server, port, username, keyFile, knownHosts);
	}
}
