/**
 * Copyright © 2011 Reinier Zwitserloot.
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
package com.zwitserloot.ivyplusplus.createProject;

import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Security;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;

import com.zwitserloot.cmdreader.CmdReader;
import com.zwitserloot.cmdreader.Description;
import com.zwitserloot.cmdreader.Excludes;
import com.zwitserloot.cmdreader.FullName;
import com.zwitserloot.cmdreader.InvalidCommandLineException;
import com.zwitserloot.cmdreader.Mandatory;
import com.zwitserloot.cmdreader.Sequential;
import com.zwitserloot.cmdreader.Shorthand;
import com.zwitserloot.ivyplusplus.Version;
import com.zwitserloot.ivyplusplus.mavencentral.CreateSigningKey;
import com.zwitserloot.ivyplusplus.mavencentral.InitializeBouncyCastle;
import com.zwitserloot.ivyplusplus.mavencentral.SigningException;
import com.zwitserloot.ivyplusplus.template.TemplateApplier;

public class CreateProject {
	private static class CmdArgs {
		@Shorthand("h")
		@Description("Show this command line help")
		boolean help;
		
		@Sequential
		@Mandatory(onlyIfNot={"help", "generate-key"})
		@Description("The name of your project. Example: com.zwitserloot.cmdreader")
		String projectName;
		
		@Shorthand("j")
		@Description("If present, a jUnit test framework will be generated.")
		boolean junit;
		
		@Shorthand("d")
		@Description("If present, javadoc ant targets will be produced.")
		boolean javadoc;
		
		@FullName("sonatype-forge")
		@Description("Creates a 'maven-build' and 'maven-upload' task that creates (and uploads) this project to Sonatype Forge. Run --generate-key to make a signing pair first if you need one.")
		boolean sonatypeForge;
		
		@Shorthand("f")
		@Description("Overwrite files if they already exist.")
		boolean force;
		
		@Shorthand("l")
		@Description("This project is not an app - do not generate a main class and dont include Main-Class in the manifest.")
		boolean library;
		
		@Description("Add a simple bsd-like license to the top of generated sources, using the supplied name as copyright holder.")
		String freeware;
		
		@Description("Generates a key pair for signing artifacts so that i.e. Sonatype Forge, which is one way to get your libraries into maven central, accepts them.")
		@FullName("generate-key")
		@Excludes({"freeware", "library", "junit", "projectName", "sonatype-forge"})
		boolean generateMavenRepoSigningKey;
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
		
		if (args.generateMavenRepoSigningKey) {
			System.exit(runGenerateMavenSigningKey());
			return;
		}
		
		TemplateApplier template = new TemplateApplier();
		
		template.put("PROJECTNAME", args.projectName);
		template.put("MIN_IPP_VERSION", Version.getVersion());
		if (args.javadoc) template.put("JAVADOC", "true");
		if (args.sonatypeForge) {
			template.put("JAVADOC", "true");
			template.put("MAVEN", "true");
		}
		if (args.junit) template.put("JUNIT", "true");
		if (args.freeware != null) {
			handleAndAddFreewareCopyright(template, args.freeware, args.force);
			template.put("MIT_LICENSE_SET", "true");
		} else {
			template.put("MIT_LICENSE_UNSET", "true");
		}
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
		if (args.sonatypeForge) {
			File docDir = new File("doc");
			docDir.mkdir();
			writeFile(template.applyResource(CreateProject.class, "maven-pom.xml.template"), new File(docDir, "maven-pom.xml").getPath(), args.force);
			System.out.println("doc/maven-pom.xml is a pom skeleton.\nYou'll need to edit it and replace all the stuff between the exclamation marks with appropriate strings.");
		}
		writeFile(template.applyResource(CreateProject.class, "Version.java.template"), new File(srcDir, "Version.java").getPath(), args.force);
	}
	
	private static int runGenerateMavenSigningKey() {
		try {
			InitializeBouncyCastle.init();
		} catch (SigningException e) {
			System.err.println(e.getMessage());
			return 12;
		}
		System.out.println(java.util.Arrays.toString(Security.getProviders()));
		System.out.println("Generating a new key pair. The generated key pair will be used by the <ivy:create-maven-artifact> task.");
		System.out.print("What is the full name of the owner of this key: ");
		Scanner s = new Scanner(System.in);
		String fullName = s.nextLine();
		System.out.print("What is the email of the owner of this key: " );
		String email = s.nextLine();
		String identity = fullName + " <" + email + ">";
		System.out.println("Key's identity: " + identity);
		System.out.println();
		System.out.println("Hit enter to set a blank passphrase.\n" +
				"This means anyone with the key ring file can sign as you,\n" +
				"so don't do this unless you know what you're doing!");
		Console console = System.console();
		String passphrase, verify;
		if (console == null) {
			System.out.print("Passphrase for this key: ");
			passphrase = s.nextLine();
			System.out.print("Repeat passphrase: ");
			verify = s.nextLine();
		} else {
			System.out.print("Passphrase for this key: ");
			passphrase = new String(console.readPassword());
			System.out.print("Repeat passphrase: ");
			verify = new String(console.readPassword());
		}
		if (!passphrase.equals(verify)) {
			System.err.println("Passwords do not match - key creation aborted.");
			return 5;
		}
		
		try {
			new CreateSigningKey().createSigningKey(identity, passphrase, System.out);
		} catch (IOException e) {
			System.err.println("Problem creating passkey files. Is the current directory writable?");
			System.err.println(e);
			return 1;
		} catch (SigningException e) {
			System.err.println("Problem creating keys: " + e.getMessage());
			return 1;
		}
		
		System.out.println("Key files created. You don't need mavenrepo-signing-key-public.bpr, but its there if you want others to be able to encrypt things so only you can read them.");
		return 0;
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
