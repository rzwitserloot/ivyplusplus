package com.zwitserloot.ivyplusplus.ecj;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.tools.StandardLocation;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Javac;
import org.apache.tools.ant.taskdefs.compilers.CompilerAdapter;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.apt.dispatch.BaseAnnotationProcessorManager;
import org.eclipse.jdt.internal.compiler.apt.dispatch.BatchAnnotationProcessorManager;
import org.eclipse.jdt.internal.compiler.apt.dispatch.BatchProcessingEnvImpl;
import org.eclipse.jdt.internal.compiler.apt.util.EclipseFileManager;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.batch.FileSystem.Classpath;
import org.eclipse.jdt.internal.compiler.batch.Main;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IModuleAwareNameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.util.Util;

public class EcjAdapter implements CompilerAdapter {
	static final byte PROJECT = 1;
	static final byte LIBRARY = 2;
	private static final String COMPILER_OPTIONS_FILE = "compiler.options.file";
	private static final String COMPILER_ARGS_SEPARATOR = "=";
	private static final String DEFAULT_COMPILER_OPTIONS_FILE = "default.compiler.options.file";
	private static final String COMPILE_PROBLEM_MESSAGE = "----------\n%s. %s in %s (at line %s)\n%s\n%s\n%s\n";
	
	private Javac javac;
	private boolean includeSystemBootclasspath;
	private Set<String> procClasses;
	private Set<File> procJars;
	
	public void setJavac(Javac javac) {
		this.javac = javac;
	}
	
	public boolean execute() throws BuildException {
		CompileJobDescriptionImpl description = new CompileJobDescriptionImpl();
		SourceFile[] sourceFiles = getSourceFilesToCompile();
		description.setSourceFiles(sourceFiles);
		description.setClasspaths(createClasspaths());
		String compilerOptionsFileName = extractJavacCompilerArg(COMPILER_OPTIONS_FILE, null);
		String defaultCompilerOptionsFileName = extractJavacCompilerArg(DEFAULT_COMPILER_OPTIONS_FILE, null);
		Map<String, String> compilerOptions = CompilerOptionsProvider.getCompilerOptions(javac, compilerOptionsFileName, defaultCompilerOptionsFileName);
		description.setCompilerOptions(compilerOptions);
		
		CompileJobResult compileJobResult = compile(description);
		
		CategorizedProblem[] categorizedProblems = compileJobResult.getCategorizedProblems();
		
		// Buffer for messages
		StringBuilder builder = new StringBuilder();
		
		boolean hasErrors = false;
		for (int i = 0; i < categorizedProblems.length; i++) {
			CategorizedProblem categorizedProblem = categorizedProblems[i];
			if (categorizedProblem.isError() || (categorizedProblem.isWarning() && !javac.getNowarn())) {
				String fileName = String.valueOf(categorizedProblem.getOriginatingFileName());
				for (SourceFile sourceFile : sourceFiles) {
					if (fileName.equals(sourceFile.getSourceFileName())) {
						Object[] args = new Object[7];
						args[0] = Integer.valueOf(i + 1);
						args[1] = categorizedProblem.isError() ? "ERROR" : "WARNING";
						args[2] = sourceFile.getSourceFile().getAbsolutePath();
						args[3] = Integer.valueOf(categorizedProblem.getSourceLineNumber());
						String[] problematicLine = readProblematicLine(sourceFile, categorizedProblem);
						args[4] = problematicLine[0];
						args[5] = problematicLine[1];
						args[6] = categorizedProblem.getMessage();
						builder.append(String.format(COMPILE_PROBLEM_MESSAGE, args));
						if (i + 1 == categorizedProblems.length) builder.append("----------\n");
						if (categorizedProblem.isError()) hasErrors = true;
					}
				}
			}
		}
		
		if (builder.length() > 0) {
			if (hasErrors) {
				javac.getProject().log("Compile errors: \n" + builder.toString(), Project.MSG_ERR);
			} else {
				if (!javac.getNowarn()) {
					javac.getProject().log("Compile warnings: \n" + builder.toString(), Project.MSG_WARN);
				}
			}
		}
		
		// if the destination directory has been specified for the javac task we might need
		// to copy the generated class files
		if (compileJobResult.succeeded() && (javac.getDestdir() != null)) {
			File destdir = getCanonicalFile(javac.getDestdir());
			try {
				cloneClasses(destdir, compileJobResult.getCompiledClassFiles());
			} catch (IOException e) {
				throw new BuildException(e);
			}
		}
		
		// throw Exception if compilation was not successful
		if (!compileJobResult.succeeded() || hasErrors) throw new BuildException("Compilation not successful");
		return true;
	}
	
	private void cloneClasses(File destDir, Map<String, File> compiledClasses) throws IOException {
		if (!destDir.isAbsolute()) destDir = destDir.getAbsoluteFile();
		for (Map.Entry<String, File> entry : compiledClasses.entrySet()) {
			File destFile = getCanonicalFile(new File(destDir, entry.getKey()));
			destFile.getParentFile().mkdirs();
			if (!destFile.equals(entry.getValue())) copy(entry.getValue(), destFile);
		}
	}
	
	public static final void copy(File source, File dest) throws IOException {
		Assure.isFile("source", source);
		Assure.notNull("to", dest);
		FileInputStream in = null;
		FileOutputStream out = null;
		FileChannel inC = null;
		FileChannel outC = null;
		
		in = new FileInputStream(source);
		out = new FileOutputStream(dest);
		inC = in.getChannel();
		outC = out.getChannel();
		inC.transferTo(0, inC.size(), outC);
		inC.close();
		outC.close();
		in.close();
		out.close();
	}
	
	private static final File getCanonicalFile(File file) {
		Assure.notNull("file", file);
		try {
			return file.getCanonicalFile();
		} catch (IOException ex) {
			return file.getAbsoluteFile();
		}
	}
	
	private String[] readProblematicLine(SourceFile sourceFile, CategorizedProblem categorizedProblem) {
		Assure.notNull("sourceFile", sourceFile);
		Assure.notNull("categorizedProblem", categorizedProblem);
		
		int lineNumber = categorizedProblem.getSourceLineNumber();
		int sourceStart = categorizedProblem.getSourceStart();
		int sourceEnd = categorizedProblem.getSourceEnd();
		
		try {
			// Open the file that is the first command line parameter
			FileInputStream fstream = new FileInputStream(sourceFile.getSourceFile());
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			int lineStart = 0;
			String strLine = "";
			// Read File Line By Line
			for (int i = 0; i < lineNumber; i++) {
				String newLine = br.readLine();
				
				lineStart = lineStart + strLine.length();
				if (i + 1 != lineNumber) lineStart = lineStart + 1;
				strLine = newLine;
			}
			in.close();
			StringBuilder underscoreLine = new StringBuilder();
			for (int i = lineStart; i < sourceStart; i++) {
				if (strLine.charAt(i - lineStart) == '\t') underscoreLine.append('\t');
				else underscoreLine.append(' ');
			}
			for (int i = sourceStart; i <= sourceEnd; i++) underscoreLine.append('^');
			return new String[] {strLine, underscoreLine.toString()};
		} catch (Exception e) {// Catch exception if any
			return new String[] {"", ""};
		}
	}
	
	private String extractJavacCompilerArg(String argumentName, String defaultValue) {
		Assure.notNull("argumentName", argumentName);
		
		// Step 1: Get all compilerArguments
		String[] currentCompilerArgs = javac.getCurrentCompilerArgs();
		
		// Step 2: Find the 'right' one
		for (String compilerArg : currentCompilerArgs) {
			// split the argument
			String[] args = compilerArg.split(COMPILER_ARGS_SEPARATOR);
			
			// requested one?
			if (args.length > 1 && argumentName.equalsIgnoreCase(args[0])) {
				return args[1];
			}
		}
		
		return defaultValue;
	}
	
	private SourceFile[] getSourceFilesToCompile() throws BuildException {
		File defaultDestinationFolder = javac.getDestdir();
		List<SourceFile> sourceFiles = new ArrayList<SourceFile>();
		
		File[] fileList = javac.getFileList();
		
		for (File file : fileList) {
			if (!hasSourceFolder(file)) {
				// the user has restricted the source folders for the compilation.
				// f.e. the project has two source folders while the user only compiles one at a time.
				continue;
			}
			
			File sourceFolder = getSourceFolder(file);
			String sourceFileName = file.getAbsolutePath().substring(sourceFolder.getAbsolutePath().length() + File.separator.length());
			File destinationFolder = defaultDestinationFolder;
			if (destinationFolder == null) throw new BuildException("dest path not set");
			
			// compile package-info.java first
			if (sourceFileName.endsWith("package-info.java")) {
				sourceFiles.add(0, SourceFileFactory.createSourceFile(sourceFolder, sourceFileName, destinationFolder, getDefaultEncoding()));
			} else {
				sourceFiles.add(SourceFileFactory.createSourceFile(sourceFolder, sourceFileName, destinationFolder, getDefaultEncoding()));
			}
		}
		return sourceFiles.toArray(new SourceFile[sourceFiles.size()]);
	}
	
	private Classpath[] createClasspaths() {
		List<Classpath> classpathList = new ArrayList<Classpath>();
		boolean includeSystem = includeSystemBootclasspath || javac.getBootclasspath() == null;
		if (javac.getBootclasspath() != null) {
			createBootClasspath(classpathList);
		}
		
		if (includeSystem) {
			String defaultBc = System.getProperty("sun.boot.class.path");
			if (defaultBc != null) {
				for (String x : defaultBc.split(File.pathSeparator)) {
					File f = new File(x);
					if (f.exists()) classpathList.add(FileSystem.getClasspath(x, "UTF-8", null));
				}
			} else {
				org.eclipse.jdt.internal.compiler.util.Util.collectVMBootclasspath(classpathList, Util.getJavaHome());
			}
		}
		
		if (javac.getClasspath() != null) {
			Iterator<? extends Resource> iterator = javac.getClasspath().iterator();
			while (iterator.hasNext()) {
				FileResource fileResource = iterator.next().as(FileResource.class);
				if (fileResource == null) continue;
				File classesFile = fileResource.getFile();
				if (classesFile.exists()) classpathList.add(FileSystem.getClasspath(classesFile.toString(), "UTF-8", null));
			}
		}
		
		return classpathList.toArray(new Classpath[0]);
	}
	
	private void createBootClasspath(List<Classpath> classpaths) {
		// Step 1: get the boot class path as specified in the javac task
		Path bootClasspath = javac.getBootclasspath();
		
		// Step 2: iterate over the boot class path entries as specified in the ant path
		for (Iterator<? extends Resource> iterator = bootClasspath.iterator(); iterator.hasNext();) {
			FileResource fileResource = iterator.next().as(FileResource.class);
			if (fileResource == null) continue;
			if (fileResource.getFile().exists()) classpaths.add(FileSystem.getClasspath(fileResource.getFile().toString(), "UTF-8", null));
		}
	}
	
	private String getDefaultEncoding() {
		String encoding = javac.getEncoding();
		if (encoding != null) return encoding;
		return "UTF-8";
	}
	
	private boolean hasSourceFolder(File sourceFile) {
		String absolutePath = sourceFile.getAbsolutePath();
		
		// get the list of all source directories
		String[] srcDirs = javac.getSrcdir().list();
		
		// find the 'right' source directory
		for (String srcDir : srcDirs) {
			if (absolutePath.startsWith(srcDir) && absolutePath.charAt(srcDir.length()) == File.separatorChar) return true;
		}
		
		return false;
	}
	
	private File getSourceFolder(File sourceFile) throws BuildException {
		String absolutePath = sourceFile.getAbsolutePath();
		String[] srcDirs = javac.getSrcdir().list();
		
		for (String srcDir : srcDirs) {
			if (absolutePath.startsWith(srcDir) && absolutePath.charAt(srcDir.length()) == File.separatorChar) return new File(srcDir);
		}
		
		throw new BuildException("Source folder for source file does not exist: " + sourceFile.getAbsolutePath());
	}
	
	static class MyFileSystem extends FileSystem {
		protected MyFileSystem(Classpath[] paths) {
			super(paths, new String[0], false);
		}
		
		@Override public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName, char[] moduleName) {
			return super.findType(typeName, packageName, moduleName);
		}
		
		@Override public NameEnvironmentAnswer findType(char[][] compoundName, char[] moduleName) {
			return super.findType(compoundName, moduleName);
		}
	}
	
	public CompileJobResult compile(CompileJobDescription description) {
		IModuleAwareNameEnvironment nameEnvironment = new MyFileSystem(description.getClasspaths());
		Map<String, String> compilerOptionsMap = description.getCompilerOptions();
		ICompilationUnit[] sources = getCompilationUnits(description.getSourceFiles());
		IErrorHandlingPolicy policy = DefaultErrorHandlingPolicies.proceedWithAllProblems();
		IProblemFactory problemFactory = new DefaultProblemFactory(Locale.getDefault());
		CompilerRequestorImpl requestor = new CompilerRequestorImpl();
		CompilerOptions compilerOptions = new CompilerOptions(compilerOptionsMap);
		compilerOptions.storeAnnotations = true;
		Compiler compiler = new Compiler(nameEnvironment, policy, compilerOptions, requestor, problemFactory);
		
		if (Boolean.getBoolean("ecj.useMultiThreading")) compiler.useSingleThread = false;
		BatchAnnotationProcessorManager aptManager = new BatchAnnotationProcessorManager();
		Main m = makeDummyMain();
		m.batchCompiler = compiler;
		List<String> aptArgs = new ArrayList<String>();
		if (!procJars.isEmpty()) {
			aptArgs.add("-processorpath");
			StringBuilder sb = new StringBuilder();
			for (File procJar : procJars) {
				if (sb.length() > 0) sb.append(File.pathSeparator);
				sb.append(procJar.getAbsolutePath());
			}
			aptArgs.add(sb.toString());
		}
		if (!procClasses.isEmpty()) {
			aptArgs.add("-processor");
			StringBuilder sb = new StringBuilder();
			for (String procClass : procClasses) {
				if (sb.length() > 0) sb.append(",");
				sb.append(procClass);
			}
			aptArgs.add(sb.toString());
		}
		
		aptManager.configure(m, aptArgs.toArray(new String[0]));
		compiler.annotationProcessorManager = aptManager;
		
		EclipseFileManager filer = null;
		
		try {
			Field f = BaseAnnotationProcessorManager.class.getDeclaredField("_processingEnv");
			f.setAccessible(true);
			BatchProcessingEnvImpl procEnv = (BatchProcessingEnvImpl) f.get(aptManager);
			filer = (EclipseFileManager) procEnv.getFileManager();
		} catch (NoSuchFieldException e) {
			// we tried - maybe it's different infra.
		} catch (IllegalAccessException e) {
			// we tried - maybe it's different infra.
		}
		
		if (filer != null) {
			Set<File> sourcePath = new HashSet<File>();
			Set<File> destinationPath = new HashSet<File>();
			for (SourceFile sf : description.getSourceFiles()) {
				sourcePath.add(sf.getSourceFolder());
				destinationPath.add(sf.getDestinationFolder());
			}
			try {
				filer.setLocation(StandardLocation.SOURCE_PATH, sourcePath);
				filer.setLocation(StandardLocation.CLASS_OUTPUT, destinationPath);
			} catch (IOException e) {
				throw new BuildException("Can't set annotation processor filer paths", e);
			}
		}
		
		compiler.compile(sources);
		CompileJobResultImpl result = new CompileJobResultImpl();
		result.setSucceeded(requestor.isCompilationSuccessful());
		result.setCategorizedProblems(requestor.getCategorizedProblems());
		result.setCompiledClassFiles(requestor.getCompiledClassFiles());
		return result;
	}
	
	private Main makeDummyMain() {
		// Make a dummy 'Main', because the ecj code is hard-linked to it existing. This is all hackery to get around
		// an utterly unusable API. This API has been replaced, but the replacement cannot process annotations,
		// unless you rewrite a ton of infrastructure.
		
		PrintWriter outWriter = new PrintWriter(new OutputStream() {
			@Override public void write(int b) throws IOException {
				// just ignore it silently.
			}
		});
		return new Main(outWriter, outWriter, false, new HashMap<String, String>(), null);
	}
	
	private ICompilationUnit[] getCompilationUnits(SourceFile[] sourceFiles) {
		List<ICompilationUnit> result = new ArrayList<ICompilationUnit>();
		for (SourceFile sourceFile : sourceFiles) {
			CompilationUnitImpl compilationUnitImpl = new CompilationUnitImpl(sourceFile);
			if (!result.contains(compilationUnitImpl)) result.add(compilationUnitImpl);
		}
		return result.toArray(new ICompilationUnit[result.size()]);
	}
	
	public void setIncludeSystemBootclasspath(boolean includeSystemBootclasspath) {
		this.includeSystemBootclasspath = includeSystemBootclasspath;
	}
	
	public void setAnnotationProcessorEntries(Set<String> procClasses, Set<File> procJars) {
		this.procClasses = procClasses;
		this.procJars = procJars;
	}
}
