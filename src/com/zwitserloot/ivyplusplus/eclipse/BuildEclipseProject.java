package com.zwitserloot.ivyplusplus.eclipse;

import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.Setter;

import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;

public class BuildEclipseProject extends IvyPostResolveTask {
	@Setter private File todir = null;
	@Setter private String projectname;
	private List<Srcdir> srcdirs = new ArrayList<Srcdir>();
	private List<Conf> confs = new ArrayList<Conf>();
	@Setter private String source = "1.6";
	@Setter private Settings settings;
	
	private void generateDotProject() throws IOException {
		File f = new File(todir, ".project");
		f.delete();
		@Cleanup
		FileOutputStream fos = new FileOutputStream(f);
		@Cleanup
		InputStream in = BuildEclipseProject.class.getResourceAsStream("project.template");
		byte[] b = new byte[4096];
		for (int r = in.read(b); r != -1; r = in.read(b)) {
			for (int i = 0; i < r; i++) {
				if (b[i] == '%') {
					fos.write(b, 0, i);
					fos.write(projectname.getBytes("UTF-8"));
					if (i < r - 1) fos.write(b, i + 1, r - i - 1);
					break;
				} else if (i == r - 1) {
					fos.write(b, 0, r);
				}
			}
		}
		in.close();
		fos.close();
	}
	
	private void generateDotClasspath(String content) throws IOException {
		File f = new File(todir, ".classpath");
		f.delete();
		@Cleanup
		FileOutputStream fos = new FileOutputStream(f);
		@Cleanup
		Writer out = new OutputStreamWriter(fos, "UTF-8");
		out.write(content);
	}
	
	public void addSrcdir(Srcdir srcdir) {
		srcdirs.add(srcdir);
	}
	
	public void addConf(Conf conf) {
		confs.add(conf);
	}
	
	public void addSettings(Settings settings) {
		if (this.settings != null) throw new BuildException("Only one <settings> allowed.", getLocation());
		this.settings = settings;
	}
	
	private static final Map<String, String> SOURCE_TO_CON;
	static {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("6", "JavaSE-1.6");
		map.put("1.6", "JavaSE-1.6");
		map.put("5", "J2SE-1.5");
		map.put("1.5", "J2SE-1.5");
		map.put("1.4", "J2SE-1.4");
		map.put("1.3", "J2SE-1.3");
		map.put("1.2", "J2SE-1.2");
		map.put("1.1", "JRE-1.1");
		SOURCE_TO_CON = unmodifiableMap(map);
	}
	
	@Override public void doExecute() throws BuildException {
		if (todir == null) todir = getProject().getBaseDir();
		for (Srcdir dir : srcdirs) {
			if (dir.getDir() == null) throw new BuildException("<srcdir> requires a 'src' attribute with the source dir you'd like to include.", getLocation());
		}
		String[] confs = new String[this.confs.size()];
		for (int i = 0; i < confs.length; i++) {
			confs[i] = this.confs.get(i).getName();
			if (confs[i] == null) {
				throw new BuildException("<conf> requires a 'name' attribute naming an ivy configuration.", getLocation());
			}
		}
		if (!SOURCE_TO_CON.containsKey(source)) throw new BuildException("Invalid value for 'source'. Valid values: " + SOURCE_TO_CON.keySet(), getLocation());
		if (projectname == null) projectname = getProject().getName();
		prepareAndCheck();
		if (settings != null) settings.execute(todir, getLocation());
		StringBuilder elements = new StringBuilder();
		String retrievePattern = getProject().getProperty("ivy.retrieve.pattern");
		assert retrievePattern != null;
		retrievePattern = IvyPatternHelper.substituteVariables(retrievePattern, getIvyInstance().getSettings().getVariables());
		
		elements.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		elements.append("<classpath>\n");
		
		for (Srcdir dir : srcdirs) {
			String path = getProject().getBaseDir().toURI().relativize(dir.getDir().toURI()).toString();
			elements.append("\t<classpathentry kind=\"src\" path=\"").append(path).append("\"");
			if (dir.isOptional()) {
				elements.append(">\n\t\t<attributes>\n\t\t\t<attribute name=\"optional\" value=\"true\"/>\n\t\t</attributes>\n\t</classpathentry>\n");
			} else {
				elements.append("/>\n");
			}
		}
		
		elements.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/")
				.append(SOURCE_TO_CON.get(source)).append("\"/>\n");
		
		ModuleDescriptor md = null;
		if (getResolveId() != null) md = (ModuleDescriptor) getResolvedDescriptor(getResolveId());
		else
			md = (ModuleDescriptor) getResolvedDescriptor(getOrganisation(), getModule(), false);
		
		IvyNode[] deps = getIvyInstance().getResolveEngine().getDependencies(md, new ResolveOptions().setConfs(confs).setResolveId(getResolveId()).setValidate(doValidate(getSettings())), null);
		for (IvyNode dep : deps) {
			for (Artifact artifact : dep.getAllArtifacts()) {
				int lowestIdx = Integer.MAX_VALUE;
				for (String config : artifact.getConfigurations()) {
					int idx = -1;
					for (int i = 0; i < confs.length; i++) {
						if (confs[i].equals(config)) {
							idx = i;
							break;
						}
					}
					if (idx == -1) continue;
					if (lowestIdx > idx) lowestIdx = idx;
				}
				if (lowestIdx == Integer.MAX_VALUE) lowestIdx = 0;
				String destFileName = IvyPatternHelper.substitute(retrievePattern, artifact.getModuleRevisionId(), artifact, confs[lowestIdx], null);
				elements.append("\t<classpathentry kind=\"lib\" path=\"").append(destFileName).append("\"/>\n");
			}
		}
		elements.append("\t<classpathentry kind=\"output\" path=\"bin\"/>\n");
		elements.append("</classpath>\n");
		try {
			generateDotProject();
			generateDotClasspath(elements.toString());
		} catch (IOException e) {
			throw new BuildException(e, getLocation());
		}
	}
}
