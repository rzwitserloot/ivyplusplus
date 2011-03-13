package com.zwitserloot.ivyplusplus.createProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.zwitserloot.cmdreader.CmdReader;
import com.zwitserloot.cmdreader.Description;
import com.zwitserloot.cmdreader.InvalidCommandLineException;
import com.zwitserloot.cmdreader.Mandatory;
import com.zwitserloot.cmdreader.Sequential;
import com.zwitserloot.cmdreader.Shorthand;
import com.zwitserloot.ivyplusplus.template.TemplateApplier;

public class CreateProject {
	private static class CmdArgs {
		@Shorthand("h")
		@Description("Show this command line help")
		boolean help;
		
		@Sequential
		@Mandatory(onlyIfNot="help")
		@Description("The name of your project. Example: com.zwitserloot.cmdreader")
		String projectName;
		
		@Shorthand("j")
		@Description("If present, a jUnit test framework will be generated.")
		boolean junit;
		
		@Shorthand("d")
		@Description("If present, javadoc ant targets will be produced.")
		boolean javadoc;
		
		@Shorthand("f")
		@Description("Overwrite files if they already exist.")
		boolean force;
		
		@Shorthand("l")
		@Description("This project is not an app - do not generate a main class and dont include Main-Class in the manifest.")
		boolean library;
		
		@Description("Add a simple bsd-like license to the top of generated sources, using the supplied name as copyright holder")
		String freeware;
	}
	
	public static void main(String[] rawArgs) throws IOException {
		CmdReader<CmdArgs> reader = CmdReader.of(CmdArgs.class);
		CmdArgs args;
		
		try {
			args = reader.make(rawArgs);
		} catch (InvalidCommandLineException e) {
			System.err.println(e.getMessage());
			System.err.println(reader.generateCommandLineHelp("java -jar ivyplusplus.jar"));
			System.exit(1);
			return;
		}
		
		if (args.help) {
			System.out.println(reader.generateCommandLineHelp("java -jar ivyplusplus.jar"));
			System.exit(0);
			return;
		}
		
		TemplateApplier template = new TemplateApplier();
		
		template.put("PROJECTNAME", args.projectName);
		template.put("MIN_IPP_VERSION", "1.4");
		if (args.javadoc) template.put("JAVADOC", "true");
		if (args.junit) template.put("JUNIT", "true");
		if (args.freeware != null) handleAndAddFreewareCopyright(template, args.freeware, args.force);
		if (!args.library) template.put("APP", "true");
		String organization, simpleName;
		{
			int idx = args.projectName.lastIndexOf('.');
			if (idx == -1) {
				organization = args.projectName;
				simpleName = args.projectName;
			} else {
				organization = reverseOnDots(args.projectName.substring(0, idx));
				simpleName = args.projectName.substring(idx + 1);
			}
		}
		template.put("ORGANIZATION", organization);
		template.put("SIMPLENAME", simpleName);
		template.put("PATH_TO_VERSIONJAVA", reverseOnDots(args.projectName).replace(".", "/") + "/Version.java");
		
		new File("buildScripts").mkdir();
		new File("buildScripts/ivy-repo").mkdir();
		writeFile(template.applyResource(CreateProject.class, "build.xml.template"), "build.xml", args.force);
		writeFile(template.applyResource(CreateProject.class, "ivy.xml.template"), "buildScripts/ivy.xml", args.force);
		writeFile(template.applyResource(CreateProject.class, "ivysettings.xml.template"), "buildScripts/ivysettings.xml", args.force);
		new File("src").mkdir();
		new File("src/main").mkdir();
		if (args.junit) new File("src/test").mkdir();
		File srcDir = new File("src/main");
		File testDir = new File("src/test");
		for (String pkgElem : reverseOnDots(args.projectName).split("\\.")) {
			srcDir = new File(srcDir, pkgElem);
			srcDir.mkdir();
			testDir = new File(testDir, pkgElem);
			if (args.junit) testDir.mkdir();
		}
		
		if (!args.library) {
			writeFile(template.applyResource(CreateProject.class, "Main.java.template"), new File(srcDir, "Main.java").getPath(), args.force);
		}
		writeFile(template.applyResource(CreateProject.class, "Version.java.template"), new File(srcDir, "Version.java").getPath(), args.force);
	}
	
	private static String reverseOnDots(String in) {
		StringBuilder sb = new StringBuilder();
		for (String elem : in.split("\\.")) {
			if (sb.length() > 0) sb.append(".");
			sb.append(elem);
		}
		return sb.toString();
	}
	
	private static void writeFile(String content, String fileName, boolean force) throws IOException {
		File f = new File(fileName);
		
		if (f.exists() && !force) {
			throw new IllegalStateException(String.format("File %s already exists. Delete it first if you want me to overwrite it, or use --force", f));
		}
		
		FileOutputStream fos = new FileOutputStream(fileName);
		try {
			fos.write(content.getBytes("UTF-8"));
		} finally {
			fos.close();
		}
	}
	
	private static void handleAndAddFreewareCopyright(TemplateApplier template, String holder, boolean force) throws IOException {
		template.put("YEAR", String.valueOf(new GregorianCalendar().get(Calendar.YEAR)));
		template.put("HOLDER", holder);
		String copyright = template.applyResource(CreateProject.class, "simpleLicense.txt.template");
		template.put("COPYRIGHT", copyright);
		String sourceCopyright = " * " + copyright.replace("\n", "\n * ");
		if (sourceCopyright.endsWith(" * ")) sourceCopyright = sourceCopyright.substring(0, sourceCopyright.length() - 3) + "\n";
		template.put("SOURCE_COPYRIGHT",sourceCopyright);
		writeFile(copyright, "LICENSE", force);
	}
}
