/**
 * Copyright © 2010-2014 Reinier Zwitserloot.
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
package com.zwitserloot.ivyplusplus.eclipse;

import static java.util.Collections.unmodifiableMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.Setter;

import org.apache.ivy.ant.IvyPostResolveTask;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ArtifactRevisionId;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.tools.ant.BuildException;

public class BuildEclipseProject extends IvyPostResolveTask {
	@Setter private File todir = null;
	@Setter private String projectname;
	private List<Srcdir> srcdirs = new ArrayList<Srcdir>();
	private List<Conf> confs = new ArrayList<Conf>();
	private List<Apt> apts = new ArrayList<Apt>();
	private List<Local> locals = new ArrayList<Local>();
	private List<Export> exports = new ArrayList<Export>();
	private List<Lib> libs = new ArrayList<Lib>();
	@Setter private String source = "1.7";
	@Setter private Settings settings;
	@Setter private boolean pde = false;
	
	private void generateDotProject() throws IOException {
		File f = new File(todir, ".project");
		f.delete();
		@Cleanup
		FileOutputStream fos = new FileOutputStream(f);
		@Cleanup
		InputStream in = BuildEclipseProject.class.getResourceAsStream(pde ? "pde_project.template" : "project.template");
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
	
	private void generateDotFactorypath() throws IOException {
		if (apts.isEmpty()) return;
		
		File f = new File(todir, ".factorypath");
		f.delete();
		@Cleanup FileOutputStream fos = new FileOutputStream(f);
		@Cleanup Writer out = new OutputStreamWriter(fos, "UTF-8");
		out.write("<factorypath>\n");
		for (Apt apt : apts) {
			String loc = apt.getLocation();
			if (loc == null) throw new BuildException("'location' attribute is required on <apt>", getLocation());
			File abs = getProject().resolveFile(loc);
			File workspace = todir == null ? getProject().getBaseDir() : todir;
			URI rel = workspace.toURI().relativize(abs.toURI());
			out.write("\t<factorypathentry kind=\"");
			out.write(rel.isAbsolute() ? "EXTJAR" : "WKSPJAR");
			out.write("\" id=\"");
			out.write(rel.isAbsolute() ? unixize(abs.getPath()) : "/" + projectname + "/" + unixize(rel.toString()));
			out.write("\" enabled=\"true\" runInBatch=\"false\"/>\n");
		}
		out.write("</factorypath>\n");
		out.close();
	}
	
	private static String unixize(String path) {
		if (File.separatorChar == '/') return path;
		return path.replace(File.separator, "/");
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
	
	public void addApt(Apt apt) {
		apts.add(apt);
	}
	
	public void addLocal(Local local) {
		locals.add(local);
	}
	
	public void addExport(Export export) {
		exports.add(export);
	}
	
	public void addLib(Lib lib) {
		libs.add(lib);
	}
	
	public void addSettings(Settings settings) {
		if (this.settings != null) throw new BuildException("Only one <settings> allowed.", getLocation());
		this.settings = settings;
	}
	
	private static final Map<String, String> SOURCE_TO_CON;
	static {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("1.7", "JavaSE-1.7");
		map.put("1.6", "JavaSE-1.6");
		map.put("1.5", "J2SE-1.5");
		map.put("1.4", "J2SE-1.4");
		map.put("1.3", "J2SE-1.3");
		map.put("1.2", "J2SE-1.2");
		map.put("1.1", "JRE-1.1");
		SOURCE_TO_CON = unmodifiableMap(map);
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
	
	private static String readProjName(File in) throws IOException {
		@Cleanup InputStream raw = new FileInputStream(in);
		BufferedReader br = new BufferedReader(new InputStreamReader(raw, "UTF-8"));
		for (String line = br.readLine(); line != null; line = br.readLine()) {
			line = line.trim();
			if (!line.startsWith("<name")) continue;
			int start = line.indexOf('>');
			if (start == -1) continue;
			int end = line.indexOf("</name>", start);
			if (end > -1) return line.substring(start + 1, end);
		}
		throw new IOException("Can't find project name from " + in.getAbsolutePath());
	}
	
	@Override public void doExecute() throws BuildException {
		if (todir == null) todir = getProject().getBaseDir();
		for (Srcdir dir : srcdirs) {
			if (dir.getDir() == null) throw new BuildException("<srcdir> requires a 'src' attribute with the source dir you'd like to include.", getLocation());
		}
		
		Map<String, String> localsToConsider = new HashMap<String, String>();
		List<String> toExport = new ArrayList<String>();
		
		for (Local local : locals) {
			if (local.getName() == null) throw new BuildException("<local> requires a 'name' attribute with a name like an ivy dependency's 'name' attribute.", getLocation());
			if (local.getOrg() == null) throw new BuildException("<local> requires an 'org' attribute with a name like an ivy dependency's 'org' attribute.", getLocation());
			String dir = local.getDir();
			if (dir == null) {
				dir = "../" + local.getOrg() + "." + local.getName();
			}
			if (new File(dir, ".project").isFile()) {
				localsToConsider.put(local.getOrg() + "." + local.getName(), dir);
			}
		}
		
		for (Export export : exports) {
			if (export.getName() == null) throw new BuildException("<export> requires a 'name' attribute with a name like an ivy dependency's 'name' attribute.", getLocation());
			if (export.getOrg() == null) throw new BuildException("<export> requires an 'org' attribute with a name like an ivy dependency's 'org' attribute.", getLocation());
			toExport.add(export.getOrg() + "." + export.getName());
		}
		
		List<String> confsWithSources = calculateConfsWithSources();
		if (!SOURCE_TO_CON.containsKey(source)) throw new BuildException("Invalid value for 'source'. Valid values: " + SOURCE_TO_CON.keySet(), getLocation());
		if (projectname == null) projectname = getProject().getName();
		prepareAndCheck();
		if (settings != null) settings.execute(todir, getLocation(), source);
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
		
		if (pde) {
			elements.append("\t<classpathentry exported=\"true\" kind=\"con\" path=\"org.eclipse.pde.core.requiredPlugins\"/>\n");
		}
		
		elements.append("\t<classpathentry kind=\"src\" path=\".apt_generated\">\n");
		elements.append("\t\t<attributes>\n\t\t\t<attribute name=\"optional\" value=\"true\"/>\n\t\t</attributes>\n");
		elements.append("\t</classpathentry>\n");
		
		ModuleDescriptor md = null;
		if (getResolveId() != null) md = (ModuleDescriptor) getResolvedDescriptor(getResolveId());
		else md = (ModuleDescriptor) getResolvedDescriptor(getOrganisation(), getModule(), false);
		
		IvyNode[] deps = getIvyInstance().getResolveEngine().getDependencies(md, new ResolveOptions()
				.setConfs(confsWithSources.toArray(new String[0])).setResolveId(getResolveId()).setValidate(doValidate(getSettings())), null);
		List<ArtifactRevisionId> handledArtifacts = new ArrayList<ArtifactRevisionId>();
		for (IvyNode dep : deps) {
			if (dep.isCompletelyEvicted()) continue;
			boolean export = toExport.contains(dep.getId().getOrganisation() + "." + dep.getId().getName());
			String exportString = export ? "exported=\"true\" " : "";
			String localDir = localsToConsider.get(dep.getId().getOrganisation() + "." + dep.getId().getName());
			if (localDir != null) {
				String projName;
				try {
					projName = readProjName(new File(localDir, ".project"));
				} catch (IOException e) {
					throw new BuildException(e.getMessage());
				}
				elements.append("\t<classpathentry " + exportString + "kind=\"src\" path=\"/").append(projName).append("\" combineaccessrules=\"false\"/>\n");
			} else for (Conf conf : confs) {
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
					elements.append("\t<classpathentry " + exportString + "kind=\"lib\" path=\"").append(destFileName).append("\"");
					if (sourceAttachment != null) {
						elements.append(" sourcepath=\"").append(sourceAttachment).append("\"");
					}
					elements.append("/>\n");
				}
			}
		}
		for (Lib lib : libs) {
			if (lib.getLocation() == null) throw new BuildException("<lib> requires 'src' attribute pointing to a jar file.", getLocation());
			String exportString = lib.export ? "exported=\"true\" " : "";
			String path = getProject().getBaseDir().toURI().relativize(lib.getLocation().toURI()).toString();
			elements.append("\t<classpathentry " + exportString + "kind=\"lib\" path=\"").append(path).append("\"/>\n");
		}
		elements.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/")
		.append(SOURCE_TO_CON.get(source)).append("\"/>\n");
		elements.append("\t<classpathentry kind=\"output\" path=\"bin\"/>\n");
		elements.append("</classpath>\n");
		try {
			generateDotProject();
			generateDotClasspath(elements.toString());
			generateDotFactorypath();
		} catch (IOException e) {
			throw new BuildException(e, getLocation());
		}
	}
}
