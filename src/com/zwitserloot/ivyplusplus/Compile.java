/**
 * Copyright © 2010-2017 Reinier Zwitserloot.
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
package com.zwitserloot.ivyplusplus;

import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DynamicAttribute;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Mkdir;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.util.facade.FacadeTaskHelper;
import org.apache.tools.ant.util.facade.ImplementationSpecificArgument;

import com.zwitserloot.ivyplusplus.ecj.EcjAdapter;

public class Compile extends MatchingTask implements DynamicAttribute {
	private UnknownElement javac, copy, mkdir;
	private Path src;
	private boolean doCopy = true;
	private boolean ecj;
	private boolean includeSystemBootclasspath;
	private String copyExcludes;
	private boolean destdirSet;
	
	public void setIncludeSystemBootclasspath(boolean includeSystemBootclasspath) {
		this.includeSystemBootclasspath = includeSystemBootclasspath;
	}
	
	public void setEcj(boolean ecj) {
		this.ecj = ecj;
	}
	
	public void setCopyExcludes(String copyExcludes) {
		this.copyExcludes = copyExcludes;
	}
	
	public void setCopy(boolean copy) {
		this.doCopy = copy;
	}
	
	public void init() {
		javac = new UnknownElement("javac");
		copy = new UnknownElement("copy");
		mkdir = new UnknownElement("mkdir");
		javac.setTaskName("compile:javac");
		copy.setTaskName("compile:copy");
		mkdir.setTaskName("compile:mkdir");
		new RuntimeConfigurable(javac, javac.getTaskName());
		new RuntimeConfigurable(copy, copy.getTaskName());
		new RuntimeConfigurable(mkdir, mkdir.getTaskName());
		javac.setProject(getProject());
		copy.setProject(getProject());
		mkdir.setProject(getProject());
	}
	
	private static final Map<String, String> JAVAC_ATTR_MAP, COPY_ATTR_MAP, MKDIR_ATTR_MAP, JAVAC_DEFAULTS, COPY_DEFAULTS;
	
	static {
		Map<String, String> m;
		
		m = new HashMap<String, String>();
		m.put("destdir", "dir");
		MKDIR_ATTR_MAP = unmodifiableMap(m);
		
		m = new HashMap<String, String>();
		m.put("destdir", "destdir");
		m.put("classpath", "classpath");
		m.put("sourcepath", "sourcepath");
		m.put("bootclasspath", "bootclasspath");
		m.put("classpathref", "classpathref");
		m.put("sourcepathref", "sourcepathref");
		m.put("bootclasspathref", "bootclasspathref");
		m.put("extdirs", "extdirs");
		m.put("encoding", "encoding");
		m.put("nowarn", "nowarn");
		m.put("debug", "debug");
		m.put("debuglevel", "debuglevel");
		m.put("deprecation", "deprecation");
		m.put("target", "target");
		m.put("source", "source");
		m.put("verbose", "verbose");
		m.put("depend", "depend");
		m.put("includeantruntime", "includeantruntime");
		m.put("includejavaruntime", "includejavaruntime");
		m.put("fork", "fork");
		m.put("executable", "executable");
		m.put("memoryinitialsize", "memoryinitialsize");
		m.put("memorymaximumsize", "memorymaximumsize");
		m.put("failonerror", "failonerror");
		m.put("errorproperty", "errorproperty");
		m.put("compiler", "compiler");
		m.put("listfiles", "listfiles");
		m.put("tempdir", "tempdir");
		m.put("updatedproperty", "updatedproperty");
		m.put("includedestclasses", "includedestclasses");
		JAVAC_ATTR_MAP = unmodifiableMap(m);
		
		m = new HashMap<String, String>();
		m.put("encoding", "UTF-8");
		m.put("debug", "on");
		m.put("target", "1.8");
		m.put("source", "1.8");
		m.put("includeantruntime", "false");
		JAVAC_DEFAULTS = unmodifiableMap(m);
		
		m = new HashMap<String, String>();
		m.put("destdir", "todir");
		m.put("preservelastmodified", "preservelastmodified");
		m.put("includeemptydirs", "includeemptydirs");
		m.put("failonerror", "failonerror");
		m.put("verbose", "verbose");
		COPY_ATTR_MAP = unmodifiableMap(m);
		
		m = new HashMap<String, String>();
		m.put("includeemptydirs", "false");
		COPY_DEFAULTS = unmodifiableMap(m);
	}
	
	private boolean setWithKey(UnknownElement elem, Map<String, String> map, String name, String value) {
		String key = map.get(name);
		if (key == null) return false;
		elem.getWrapper().setAttribute(key, value);
		return true;
	}
	
	public void setDynamicAttribute(String name, String value) throws BuildException {
		boolean matched = false;
		matched |= setWithKey(mkdir, MKDIR_ATTR_MAP, name, value);
		matched |= setWithKey(javac, JAVAC_ATTR_MAP, name, value);
		matched |= setWithKey(copy, COPY_ATTR_MAP, name, value);
		if (!matched) throw new BuildException("Unknown property of compile task: " + name, getLocation());
		if ("destdir".equals(name)) destdirSet = true;
	}
	
	public void setSrcdir(Path srcDir) {
		if (src == null) src = srcDir;
		else src.append(srcDir);
	}
	
	public Path createSrc() {
		if (src == null) {
			src = new Path(getProject());
		}
		return src.createPath();
	}
	
	private Path compileClasspath, compileSourcepath, bootclasspath, extdirs;
	
	public Path createSourcepath() {
		if (compileSourcepath == null) compileSourcepath = new Path(getProject());
		return compileSourcepath.createPath();
	}
	
	public Path createClasspath() {
		if (compileClasspath == null) compileClasspath = new Path(getProject());
		return compileClasspath.createPath();
	}
	
	public Path createBootclasspath() {
		if (bootclasspath == null) bootclasspath = new Path(getProject());
		return bootclasspath.createPath();
	}
	
	public Path createExtdirs() {
		if (extdirs == null) extdirs = new Path(getProject());
		return extdirs.createPath();
	}
	
	private List<ImplementationSpecificArgument> compilerArgs = new ArrayList<ImplementationSpecificArgument>();
	
	public ImplementationSpecificArgument createCompilerArg() {
		ImplementationSpecificArgument arg = new ImplementationSpecificArgument();
		compilerArgs.add(arg);
		return arg;
	}
	
	public void execute() {
		if (!destdirSet) throw new BuildException("mandatory property 'destdir' not set.");
		if (src == null) src = new Path(getProject());
		Map<?, ?> attributeMap = javac.getWrapper().getAttributeMap();
		for (Map.Entry<String, String> e : JAVAC_DEFAULTS.entrySet()) {
			if (!attributeMap.containsKey(e.getKey())) javac.getWrapper().setAttribute(e.getKey(), e.getValue());
		}
		attributeMap = copy.getWrapper().getAttributeMap();
		for (Map.Entry<String, String> e : COPY_DEFAULTS.entrySet()) {
			if (!attributeMap.containsKey(e.getKey())) copy.getWrapper().setAttribute(e.getKey(), e.getValue());
		}
		
		mkdir.maybeConfigure();
		Mkdir mkdirTask = (Mkdir) mkdir.getRealThing();
		mkdirTask.execute();
		
		javac.maybeConfigure();
		Javac javacTask = (Javac) javac.getRealThing();
		javacTask.setSrcdir(src);
		javacTask.createCompilerArg().setValue("-Xlint:unchecked");
		if (bootclasspath != null) javacTask.setBootclasspath(bootclasspath);
		if (compileClasspath != null) javacTask.setClasspath(compileClasspath);
		if (compileSourcepath != null) javacTask.setSourcepath(compileSourcepath);
		if (extdirs != null) javacTask.setExtdirs(extdirs);
		try {
			Field f = MatchingTask.class.getDeclaredField("fileset");
			f.setAccessible(true);
			f.set(javacTask, getImplicitFileSet().clone());
			f = Javac.class.getDeclaredField("facade");
			f.setAccessible(true);
			FacadeTaskHelper facade = (FacadeTaskHelper) f.get(javacTask);
			for (ImplementationSpecificArgument isa : compilerArgs) facade.addImplementationArgument(isa);
		} catch (Exception e) {
			throw new BuildException(e, getLocation());
		}
		if (ecj) {
			EcjAdapter ecjAdapter = new EcjAdapter();
			if (includeSystemBootclasspath) ecjAdapter.setIncludeSystemBootclasspath(true);
			javacTask.add(ecjAdapter);
		} else {
			if (includeSystemBootclasspath) throw new BuildException("includeSystemBootclasspath only supported in combination with ecj=\"true\"");
		}
		javacTask.execute();
		
		if (doCopy) {
			copy.maybeConfigure();
			Copy copyTask = (Copy) copy.getRealThing();
			for (String pathElem : src.list()) {
				File srcPath = getProject().resolveFile(pathElem);
				FileSet fs = (FileSet) getImplicitFileSet().clone();
				fs.setDir(srcPath);
				fs.createExclude().setName("**/*.java");
				if (copyExcludes != null) fs.createExclude().setName(copyExcludes);
				copyTask.addFileset(fs);
			}
			copyTask.execute();
		}
	}
}
