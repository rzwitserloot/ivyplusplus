/**
 * Copyright © 2010-2020 Reinier Zwitserloot.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.apache.tools.ant.Project;

public class BuildEclipseProject extends IvyPostResolveTask {
	private File todir = null;
	private String projectname;
	private List<Srcdir> srcdirs = new ArrayList<Srcdir>();
	private List<Conf> confs = new ArrayList<Conf>();
	private List<Apt> apts = new ArrayList<Apt>();
	private List<Local> locals = new ArrayList<Local>();
	private List<Projectdep> projectdeps = new ArrayList<Projectdep>();
	private List<Export> exports = new ArrayList<Export>();
	private List<Lib> libs = new ArrayList<Lib>();
	private String source = "1.8";
	private String srcout = "bin";
	private Settings settings;
	private boolean pde = false;
	
	public void setTodir(File todir) {
		this.todir = todir;
	}
	
	public void setProjectname(String projectname) {
		this.projectname = projectname;
	}
	
	public void setSrcout(String srcout) {
		this.srcout = srcout;
	}
	
	public void setSource(String source) {
		this.source = source;
	}
	
	public void setSettings(Settings settings) {
		this.settings = settings;
	}
	
	public void setPde(boolean pde) {
		this.pde = pde;
	}
	
	private void generateDotProject() throws IOException {
		File f = new File(todir, ".project");
		f.delete();
		FileOutputStream fos = new FileOutputStream(f);
		try {
			InputStream in = BuildEclipseProject.class.getResourceAsStream(pde ? "pde_project.template" : "project.template");
			try {
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
			} finally {
				in.close();
			}
		} finally {
			fos.close();
		}
	}
	
	private void generateDotFactorypath() throws IOException {
		if (apts.isEmpty()) return;
		
		File f = new File(todir, ".factorypath");
		f.delete();
		FileOutputStream fos = new FileOutputStream(f);
		try {
			Writer out = new OutputStreamWriter(fos, "UTF-8");
			try {
				out.write("<factorypath>\n");
				for (Apt apt : apts) {
					EclipsePath ep = eclipsify(todir, apt.getLocation(), true);
					if (ep == null) throw new BuildException("'location' attribute is required on <apt>", getLocation());
					out.write("\t<factorypathentry kind=\"");
					out.write(ep.isExternal() ? "EXTJAR" : "WKSPJAR");
					out.write("\" id=\"");
					out.write(ep.getPath());
					out.write("\" enabled=\"true\" runInBatch=\"false\"/>\n");
				}
				out.write("</factorypath>\n");
			} finally {
				out.close();
			}
		} finally {
			fos.close();
		}
	}
	
	private static final class EclipsePath {
		private final boolean external;
		private final String path;
		
		public EclipsePath(boolean external, String path) {
			this.external = external;
			this.path = path;
		}
		
		@Override public String toString() {
			return path;
		}
		
		public String getPath() {
			return path;
		}
		
		public boolean isExternal() {
			return external;
		}
		
		@Override public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (external ? 1231 : 1237);
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			return result;
		}
		
		@Override public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			EclipsePath other = (EclipsePath) obj;
			if (external != other.external) return false;
			if (path == null) {
				if (other.path != null) return false;
			} else if (!path.equals(other.path)) return false;
			return true;
		}
	}
	
	private EclipsePath eclipsify(File base, String loc, boolean forceProject) {
		if (loc == null) return null;
		return eclipsify(base, new File(loc), forceProject);
	}
	
	private EclipsePath eclipsify(File base, File loc, boolean forceProject) {
		if (loc == null) return null;
		
		/* Check if the folder shared by baseUri and locUri is the direct parent AND the next dir for locUri is an eclipse project.
		 * If that is the case, go project relative (assume that project is also in the workspace). */ {
			int lastSlash = -1;
			String b = canonical(base);
			String c = canonical(loc);
			
			int min = Math.min(b.length(), c.length());
			int i = 0;
			for (; i < min; i++) {
				char bc = b.charAt(i);
				char cc = c.charAt(i);
				if (bc != cc) break;
				if (bc == '/') lastSlash = i;
			}
			
			if (i == b.length() && c.length() > i && c.charAt(i) == '/') {
				return new EclipsePath(false, (forceProject ? ("/" + projectname + "/") : "") + c.substring(i + 1));
			}
			
			int lastSlashInBase = b.indexOf('/', lastSlash + 1);
			if (lastSlashInBase == -1 || lastSlashInBase == b.length() - 1) {
				// This means the shared root is indeed the direct parent of base.
				// Next requirement: the first subdir after this in 'loc' must be an eclipse project.
				String siblingProjectName = fetchSiblingProjectName(c, lastSlash);
				if (siblingProjectName != null) {
					int nextSlashInLoc = c.indexOf('/', lastSlash + 1);
					if (nextSlashInLoc != -1) {
						return new EclipsePath(false, "/" + siblingProjectName + "/" + c.substring(nextSlashInLoc + 1));
					}
				}
			}
		}
		
		/* Give up, going with an absolute path. */ {
			return new EclipsePath(true, canonical(loc));
		}
	}
	
	private String fetchSiblingProjectName(String resourcePath, int workspaceRootPos) {
		int nextSlashInLoc = resourcePath.indexOf('/', workspaceRootPos + 1);
		if (nextSlashInLoc == -1) return null;
		File siblingProj = new File(resourcePath.substring(0, nextSlashInLoc), ".project");
		if (!siblingProj.isFile()) return null;
		try {
			return readProjName(siblingProj, true);
		} catch (IOException e) {
			warnAboutEclipseProjectReadFailure(false, siblingProj);
			return null;
		}
	}
	
	private final List<String> alreadyWarnedForEclipseProjectReadFailure = new ArrayList<String>();
	private String warnAboutEclipseProjectReadFailure(boolean error, File resourceFile) {
		String resourcePath = canonical(resourceFile);
		if (error) throw new BuildException("Cannot parse eclipse project file: " + resourcePath);
		
		if (alreadyWarnedForEclipseProjectReadFailure.contains(resourcePath)) return null;
		alreadyWarnedForEclipseProjectReadFailure.add(resourcePath);
		
		getProject().log("Can't read eclispe project file: " + resourcePath, Project.MSG_WARN);
		return null;
	}
	
	private String canonical(File f) {
		try {
			return f.getCanonicalPath();
		} catch (IOException e) {
			return f.getAbsolutePath();
		}
	}

	private void generateDotClasspath(String content) throws IOException {
		File f = new File(todir, ".classpath");
		f.delete();
		FileOutputStream fos = new FileOutputStream(f);
		try {
			Writer out = new OutputStreamWriter(fos, "UTF-8");
			try {
				out.write(content);
			} finally {
				out.close();
			}
		} finally {
			fos.close();
		}
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
	
	public void addProjectdep(Projectdep projectdep) {
		projectdeps.add(projectdep);
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
		map.put("25", "JavaSE-25");
		map.put("24", "JavaSE-24");
		map.put("23", "JavaSE-23");
		map.put("22", "JavaSE-22");
		map.put("21", "JavaSE-21");
		map.put("20", "JavaSE-20");
		map.put("19", "JavaSE-19");
		map.put("18", "JavaSE-18");
		map.put("17", "JavaSE-17");
		map.put("16", "JavaSE-16");
		map.put("15", "JavaSE-15");
		map.put("14", "JavaSE-14");
		map.put("13", "JavaSE-13");
		map.put("12", "JavaSE-12");
		map.put("11", "JavaSE-11");
		map.put("10", "JavaSE-10");
		map.put("9", "JavaSE-9");
		map.put("1.8", "JavaSE-1.8");
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
	
	private String readProjName(File in, boolean error) throws IOException {
		try (
			FileInputStream fis = new FileInputStream(in);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
		) {
			List<String> stack = new ArrayList<String>();
			while (true) {
				String line = br.readLine();
				if (line == null) return null;
				line = line.trim();
				if (!line.startsWith("<")) return warnAboutEclipseProjectReadFailure(error, in);
				int idx = line.indexOf('>');
				if (idx == -1) return warnAboutEclipseProjectReadFailure(error, in);
				if (line.length() > 4 && line.charAt(1) == '?' && line.charAt(line.length() - 2) == '?' && line.charAt(line.length() -1) == '>') continue;
				String tag = line.substring(1, idx);
				
				if (tag.startsWith("/")) {
					if (stack.size() == 0) return warnAboutEclipseProjectReadFailure(error, in);
					if (!stack.get(stack.size() - 1).equals(tag)) return warnAboutEclipseProjectReadFailure(error, in);
					stack.remove(stack.size() - 1);
					continue;
				}
				
				if (idx == line.length() - 1) {
					stack.add(tag);
					continue;
				}
				
				if (line.endsWith("</" + tag + ">")) {
					String text = line.substring(idx + 1, line.length() - 3 - tag.length());
					if (text.indexOf('<') != -1 || text.indexOf('>') != -1) return warnAboutEclipseProjectReadFailure(error, in);
					if (!tag.equals("name")) continue;
					if (stack.size() == 1 && stack.get(0).equals("projectDescription")) return text;
					continue;
				}
				
				return warnAboutEclipseProjectReadFailure(error, in);
			}
		}
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
			String path = todir.toURI().relativize(dir.getDir().toURI()).toString();
			elements.append("\t<classpathentry kind=\"src\" ");
			if (!dir.getSrcout().isEmpty()) {
				elements.append("output=\"");
				elements.append(dir.getSrcout());
				elements.append("\" ");
			}
			elements.append("path=\"").append(path).append("\"");
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
		
		for (Projectdep pd : projectdeps) {
			elements.append("\t<classpathentry kind=\"src\" path=\"/").append(pd.getName()).append("\" combineaccessrules=\"false\"/>\n");
		}
		
		ModuleDescriptor md = getResolveId() != null ?
			(ModuleDescriptor) getResolvedDescriptor(getResolveId()) :
			(ModuleDescriptor) getResolvedDescriptor(getOrganisation(), getModule(), false);
		
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
					projName = readProjName(new File(localDir, ".project"), true);
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
					
					EclipsePath libPath = eclipsify(todir, destFileName, false);
					EclipsePath relSrcAtt = eclipsify(todir, sourceAttachment, false);
					elements.append("\t<classpathentry " + exportString + "kind=\"lib\" path=\"").append(libPath.getPath()).append("\"");
					if (relSrcAtt != null) {
						elements.append(" sourcepath=\"").append(relSrcAtt.getPath()).append("\"");
					}
					elements.append("/>\n");
				}
			}
		}
		for (Lib lib : libs) {
			if (lib.getLocation() == null) throw new BuildException("<lib> requires 'src' attribute pointing to a jar file.", getLocation());
			String exportString = lib.export ? "exported=\"true\" " : "";
			EclipsePath path = eclipsify(todir, lib.getLocation(), false);
			elements.append("\t<classpathentry " + exportString + "kind=\"lib\" path=\"").append(path.getPath()).append("\"/>\n");
		}
		elements.append("\t<classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/")
		.append(SOURCE_TO_CON.get(source)).append("\"/>\n");
		elements.append("\t<classpathentry kind=\"output\" path=\"" + srcout + "\"/>\n");
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
