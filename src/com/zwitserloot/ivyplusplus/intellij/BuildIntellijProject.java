/**
 * Copyright © 2010 Reinier Zwitserloot.
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
package com.zwitserloot.ivyplusplus.intellij;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;

public class BuildIntellijProject extends IvyPostResolveTask {
	private File todir = null;
	private List<Module> modules = new ArrayList<Module>();
	private List<Conf> confs = new ArrayList<Conf>();
	private Apt apt;
	private String source = "1.8";
	private Settings settings;
	
	public void setTodir(File todir) {
		this.todir = todir;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public void setSettings(Settings settings) {
		this.settings = settings;
	}
	
	public void addConf(Conf conf) {
		confs.add(conf);
	}
	
	public void addApt(Apt apt) {
		if (this.apt != null) throw new BuildException("Only one <apt> allowed.", getLocation());
		this.apt = apt;
	}
	
	public void addSettings(Settings settings) {
		if (this.settings != null) throw new BuildException("Only one <settings> allowed.", getLocation());
		this.settings = settings;
	}
	
	public void addModule(Module module) {
		modules.add(module);
	}
	
	@Override public void doExecute() throws BuildException {
		if (todir == null) todir = getProject().getBaseDir();
		for (Module m : modules) m.validate(getLocation());
		try {
			generateAntXml(todir);
			generateCompilerXml(todir, apt != null && apt.isEnabled());
			generateLibraryXml(todir);
			generateModulesXml(todir);
			for (Module m : modules) {
				generateModuleXml(todir, m);
			}
		} catch (IOException e) {
			throw new BuildException(e, getLocation());
		}
	}
	
	private void generateAntXml(File toDir) throws IOException {
		applyTemplate("ant.xml.template", new File(toDir, ".idea/ant.xml"));
	}
	
	private void generateCompilerXml(File toDir, boolean isAptEnabled) throws IOException {
		applyTemplate("compiler.xml.template", new File(toDir, ".idea/compiler.xml"), isAptEnabled ? "true" : "false");
	}
	
	private void generateModulesXml(File toDir) throws IOException {
		StringBuilder moduleLines = new StringBuilder();
		for (Module m : modules) {
			moduleLines.append("      <module fileurl=\"file://$PROJECT_DIR$/").append(m.getName()).append(".iml\" filepath=\"$PROJECT_DIR$/").append(m.getName()).append(".iml\" />\n");
		}
		applyTemplate("modules.xml.template", new File(toDir, ".idea/modules.xml"), moduleLines.toString());
	}
	
	private List<String> calculateConfsWithSources() {
		List<String> out = new ArrayList<String>();
		for (Conf conf : confs) {
			String confName = conf.getName();
			if (confName == null) throw new BuildException("<conf> requires a 'name' attribute naming an ivy configuration.", getLocation());
			if (!out.contains(confName)) out.add(confName);
			String sourcesName = conf.getSources();
			if (sourcesName != null && !out.contains(sourcesName)) out.add(sourcesName);
		}
		return out;
	}
	
	private void generateLibraryXml(File toDir) throws IOException {
		ModuleDescriptor md = null;
		if (getResolveId() != null) md = (ModuleDescriptor) getResolvedDescriptor(getResolveId());
		else md = (ModuleDescriptor) getResolvedDescriptor(getOrganisation(), getModule(), false);
		
		prepareAndCheck();
		if (settings != null) settings.execute(todir, getLocation(), source);
		
		List<String> confsWithSources = calculateConfsWithSources();
		String retrievePattern = getProject().getProperty("ivy.retrieve.pattern");
		assert retrievePattern != null;
		
		IvyNode[] deps = getIvyInstance().getResolveEngine().getDependencies(md, new ResolveOptions()
				.setConfs(confsWithSources.toArray(new String[0])).setResolveId(getResolveId()).setValidate(doValidate(getSettings())), null);
		List<ArtifactRevisionId> handledArtifacts = new ArrayList<ArtifactRevisionId>();
		Map<Conf, StringBuilder> depLines = new HashMap<Conf, StringBuilder>();
		Map<Conf, StringBuilder> sourceLines = new HashMap<Conf, StringBuilder>();
		for (IvyNode dep : deps) {
			for (Conf conf : confs) {
				for (Artifact artifact : dep.getArtifacts(conf.getName())) {
					if (handledArtifacts.contains(artifact.getId())) continue;
					handledArtifacts.add(artifact.getId());
					if (!"jar".equals(artifact.getType()) && !"bundle".equals(artifact.getType())) continue;
					String destFileName = IvyPatternHelper.substitute(retrievePattern, artifact.getModuleRevisionId(), artifact, conf.getName(), null);
					String sourceConf = conf.getSources();
					String sourceAttachment = null;
					if (sourceConf != null) for (Artifact sourceArtifact : dep.getArtifacts(sourceConf)) {
						sourceAttachment = IvyPatternHelper.substitute(retrievePattern, sourceArtifact.getModuleRevisionId(), sourceArtifact, sourceConf, null);
						break;
					}
					StringBuilder sb = depLines.get(conf);
					if (sb == null) {
						sb = new StringBuilder();
						depLines.put(conf, sb);
					}
					sb.append("      <root url=\"jar://$PROJECT_DIR$/").append(destFileName).append("!/\" />\n");
					if (sourceAttachment != null) {
						sb = sourceLines.get(conf);
						if (sb == null) {
							sb = new StringBuilder();
							sourceLines.put(conf, sb);
						}
						sb.append("      <root url=\"jar://$PROJECT_DIR$/").append(sourceAttachment).append("!/\" />\n");
					}
				}
			}
		}
		
		for (Conf conf : confs) {
			String depLine = depLines.get(conf) == null ? "" : depLines.get(conf).toString();
			String sourceLine = sourceLines.get(conf) == null ? "" : sourceLines.get(conf).toString();
			applyTemplate("librarySet.xml.template", new File(toDir, ".idea/libraries/" + conf.getName() + "_libs.xml"), conf.getName() + " libs", depLine, sourceLine);
		}
	}
	
	private void generateModuleXml(File toDir, Module m) throws IOException {
		//       <sourceFolder url="file://$MODULE_DIR$/src" isTestSource="false" />
		//    <orderEntry type="library" name="build libs" level="project" />
		
		StringBuilder sourceDirs = new StringBuilder();
		StringBuilder libSets = new StringBuilder();
		
		for (Srcdir dir : m.getSrcdirs()) {
			String path = getProject().getBaseDir().toURI().relativize(dir.getDir().toURI()).toString();
			sourceDirs.append("      <sourceFolder url=\"file://$MODULE_DIR$/").append(path).append("\" isTestSource=\"").append(dir.isTest() ? "true" : "false").append("\" />\n");
		}
		
		String deps = m.getDepends();
		if (deps == null) deps = "";
		for (String dep : deps.split(",")) {
			dep = dep.trim();
			libSets.append("    <orderEntry type=\"library\" name=\"").append(dep).append(" libs\" level=\"project\" />\n");
		}
		
		applyTemplate("module.xml.template", new File(toDir, m.getName() + ".iml"), sourceDirs.toString(), source, libSets.toString());
	}
	
	private static final String MARKER = "%%";
	private static void applyTemplate(String resource, File out, String... replacements) throws IOException {
		out.delete();
		out.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(out);
		try {
			InputStream in = BuildIntellijProject.class.getResourceAsStream(resource);
			try {
				byte[] b = new byte[4096];
				int state = 0;
				for (int r = in.read(b); r != -1; r = in.read(b)) {
					int start = 0;
					for (int i = 0; i < r; i++) {
						if (b[i] == '%') {
							if (state == 0) {
								fos.write(b, start, i - start);
								start = i;
							}
							
							if (++state > MARKER.length()) {
								state--; start++;
								fos.write('%');
							}
						} else if (state == MARKER.length() && Character.isDigit(b[i])) {
							start = i + 1;
							fos.write(replacements[b[i] - '1'].getBytes("UTF-8"));
							state = 0;
						} else if (state > 0) {
							state = 0;
							fos.write(b, start, i - start);
							start = i;
						}
					}
					if (start < r) {
						fos.write(b, start, r - start);
					}
				}
			} finally {
				in.close();
			}
		} finally {
			fos.close();
		}
	}
}
