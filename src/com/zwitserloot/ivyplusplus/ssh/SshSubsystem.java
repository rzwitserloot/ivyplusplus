package com.zwitserloot.ivyplusplus.ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.taskdefs.Redirector;
import org.apache.tools.ant.types.Path;

public class SshSubsystem {
	public static void unpack(File loc) throws IOException {
		loc.mkdirs();
		File f = new File(loc, "HASH");
		String existingHash = null;
		if (f.exists()) existingHash = fetchHash(f);
		
		JarUnpacker.unpack(loc, existingHash);
	}
	
	private static String fetchHash(File f) throws IOException {
		try (InputStream in = new FileInputStream(f)) {
			return new BufferedReader(new InputStreamReader(in, "UTF-8")).readLine();
		}
	}
	
	private static final class DummyInputStream extends InputStream {
		@Override public int read() throws IOException {
			return -1;
		}
		
		@Override public int read(byte[] b) throws IOException {
			return -1;
		}
		
		@Override public int read(byte[] b, int off, int len) throws IOException {
			return -1;
		}
	}
	
	private static final class CmdHandlerStream extends OutputStream {
		private final StringBuilder line = new StringBuilder();
		private final Project p;
		private final Task t;
		
		CmdHandlerStream(Project p, Task t) {
			this.p = p;
			this.t = t;
		}
		
		@Override public void write(int b) throws IOException {
			if (b == '\n') {
				if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') line.setLength(line.length() - 1);
				String txt = line.toString();
				line.setLength(0);
				if (txt.startsWith("T:")) {
					p.log(t, txt.substring(2), Project.MSG_ERR);
				} else if (txt.startsWith("V:")) {
					p.log(t, txt.substring(2), Project.MSG_VERBOSE);
				} else if (txt.startsWith("C:")) {
					p.log(t, txt.substring(2), Project.MSG_INFO);
				} else if (txt.trim().length() > 0) {
					p.log(t, txt, Project.MSG_INFO);
				}
			} else {
				line.append((char) b);
			}
		}
	}
	
	public static void call(File loc, final Project p, final Task t, String classSpec, Object... args) {
		final Java task = new Java() {
			@Override protected void setupRedirector() {
				this.redirector = new Redirector(this) {
					private CmdHandlerStream out, err;
					private InputStream in;
					
					@Override public void createStreams() {
						out = new CmdHandlerStream(p, t);
						err = new CmdHandlerStream(p, t);
						in = new DummyInputStream();
					}
					@Override public OutputStream getOutputStream() {
						return out;
					}
					@Override public OutputStream getErrorStream() {
						return err;
					}
					@Override public InputStream getInputStream() {
						return in;
					}
					@Override public void complete() throws IOException {
						out.write('\n');
						err.write('\n');
					}
				};
				super.setupRedirector();
			}
		};
		task.setProject(p);
		task.setFork(true);
		task.setTaskName("ssh");
		task.createJvmarg().setValue("-Dorg.slf4j.simpleLogger.defaultLogLevel=warn");
		Path cp = task.createClasspath();
		cp.add(new Path(p, new File(loc, "classes").getAbsolutePath()));
		for (File jar : new File(loc, "lib").listFiles()) {
			if (jar.isFile() && jar.getName().endsWith(".jar")) cp.add(new Path(p, new File(new File(loc, "lib"), jar.getName()).getAbsolutePath()));
		}
		for (Object a : args) {
			if (a instanceof File) task.createArg().setValue(((File) a).getAbsolutePath());
			else task.createArg().setValue(a.toString());
		}
		task.setClassname(classSpec);
		task.execute();
	}
}
