/**********************************************************************
 * Copyright (c) 2005-2009 ant4eclipse project team.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nils Hartmann, Daniel Kasmeroglu, Gerd Wuetherich
 **********************************************************************/
package com.zwitserloot.ivyplusplus.ecj;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.util.Util;

/**
 * The {@link CompilerOptionsProvider} is a utility class that computes compiler
 * options based on ant's javac task as well as an (optional) project specific
 * and an (optional) global compiler options file.
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class CompilerOptionsProvider {
	/**
	 * This property enables the javadoc parsing in ECJ (same as <tt>-enableJavadoc</tt> on the command line)
	 */
	public static final String ENABLE_JAVADOC_SUPPORT = "org.eclipse.jdt.core.compiler.doc.comment.support";
	public static final String FORBIDDEN_REFERENCE = "org.eclipse.jdt.core.compiler.problem.forbiddenReference";
	
	/** prefix used for the exported preferences */
	private static final String PREFS_INSTANCE = "/instance/";
	
	/** we're only interested in jdt settings */
	private static final String PREFS_JDTTYPE = "org.eclipse.jdt.core/";
	
	/**
	 * Creates the compiler options for the JDT compiler.
	 * 
	 * The compiler options are defined here:
	 * <ul>
	 * <li><a href=
	 * "http://help.eclipse.org/galileo/topic/org.eclipse.jdt.doc.isv/guide/jdt_api_options.htm">JDT
	 * Core options</a></li>
	 * <li><a href=
	 * "http://help.eclipse.org/galileo/topic/org.eclipse.jdt.doc.user/reference/preferences/java/ref-preferences-compiler.htm"
	 * >Java Compiler Preferences </a></li>
	 * <li><a href=
	 * "http://help.eclipse.org/galileo/topic/org.eclipse.jdt.doc.user/reference/preferences/java/compiler/ref-preferences-errors-warnings.htm"
	 * >Java Compiler Errors/Warnings Preferences</a></li>
	 * </ul>
	 * 
	 * @param javac The javac task.
	 * @param projectCompilerOptionsFile The project specific compiler options file.
	 * @param globalCompilerOptionsFile The global compiler options file.
	 * @return the map with the merged compiler options.
	 */
	public static final Map<String, String> getCompilerOptions(Javac javac, String projectCompilerOptionsFile, String globalCompilerOptionsFile) {
		Assure.notNull("javac", javac);
		
		// get the project options
		Map<String, String> projectOptions = getFileCompilerOptions(projectCompilerOptionsFile);
		// get the default options
		Map<String, String> defaultOptions = getFileCompilerOptions(globalCompilerOptionsFile);
		// get the javac options
		Map<String, String> javacOptions = getJavacCompilerOptions(javac);
		// merge the map
		Map<String, String> mergedMap = mergeCompilerOptions(projectOptions, defaultOptions, javacOptions);
		
		// [AE-201] If not enabled/disabled explicitly, enable ECJ javadoc parsing support to find references inside javadoc
		if (!mergedMap.containsKey(ENABLE_JAVADOC_SUPPORT)) mergedMap.put(ENABLE_JAVADOC_SUPPORT, "enabled");
		
		// If not enabled/disabled explicitly, set ECJ forbidden reference to 'error'
		if (!mergedMap.containsKey(FORBIDDEN_REFERENCE)) mergedMap.put(FORBIDDEN_REFERENCE, "error");
		
		CompilerOptions compilerOptions = new CompilerOptions(mergedMap);
		compilerOptions.verbose = javac.getVerbose();
		
		// return the compiler options
		Map<String, String> result = new HashMap<String, String>();
		result.putAll(compilerOptions.getMap());
		return result;
	}
	
	/**
	 * Returns the compiler options specified in the javac task.
	 * 
	 * @param javac the javac task
	 * @return the compiler options specified in the javac task.
	 */
	@SuppressWarnings("unchecked") private static final Map<String, String> getJavacCompilerOptions(Javac javac) {
		Map<String, String> result = new HashMap<String, String>();
		
		if (javac.getSource() != null && !javac.getSource().isEmpty()) {
			String source = javac.getSource();
			if (source.equals("1.3")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_3);
			} else if (source.equals("1.4")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_4);
			} else if (source.equals("1.5") || source.equals("5") || source.equals("5.0")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
			} else if (source.equals("1.6") || source.equals("6") || source.equals("6.0")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_6);
			} else if (source.equals("1.7") || source.equals("7") || source.equals("7.0")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7);
			} else if (source.equals("1.8") || source.equals("8") || source.equals("8.0")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_8);
			} else if (source.equals("9") || source.equals("9.0")) {
				result.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_9);
			} else {
				throw new BuildException("Unknown java source: " + source);
			}
		}
		
		if (javac.getTarget() != null && !javac.getTarget().isEmpty()) {
			String target = javac.getTarget();
			if (target.equals("1.3")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_3);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_3);
			} else if (target.equals("1.4")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);
			} else if (target.equals("1.5") || target.equals("5") || target.equals("5.0")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
			} else if (target.equals("1.6") || target.equals("6") || target.equals("6.0")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_6);
			} else if (target.equals("1.7") || target.equals("7") || target.equals("7.0")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_7);
			} else if (target.equals("1.8") || target.equals("8") || target.equals("8.0")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_8);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_8);
			} else if (target.equals("9") || target.equals("9.0")) {
				result.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_9);
				result.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_9);
			} else {
				throw new BuildException("Unknown java target: " + target);
			}
		}
		
		if (javac.getDebug()) {
			String debugLevel = javac.getDebugLevel();
			
			if (debugLevel != null) {
				result.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.DO_NOT_GENERATE);
				result.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.DO_NOT_GENERATE);
				result.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.DO_NOT_GENERATE);
				if (debugLevel.length() != 0) {
					if (debugLevel.indexOf("vars") != -1) {
						result.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
					}
					if (debugLevel.indexOf("lines") != -1) {
						result.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
					}
					if (debugLevel.indexOf("source") != -1) {
						result.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
					}
				}
			} else {
				result.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
				result.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
				result.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
			}
		} else {
			result.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.DO_NOT_GENERATE);
			result.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.DO_NOT_GENERATE);
			result.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.DO_NOT_GENERATE);
		}
		
		/*
		 * Handle the nowarn option. If none, then we generate all warnings.
		 */
		if (javac.getNowarn()) {
			// disable all warnings
			Map.Entry<String, String>[] entries = result.entrySet().toArray(new Map.Entry[result.size()]);
			for (Entry<String, String> entrie : entries) {
				Map.Entry<String, String> entry = entrie;
				if (entry.getValue().equals(CompilerOptions.WARNING)) {
					result.put(entry.getKey(), CompilerOptions.IGNORE);
				}
			}
			result.put(CompilerOptions.OPTION_TaskTags, Util.EMPTY_STRING);
			if (javac.getDeprecation()) {
				result.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.WARNING);
				result.put(CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode, CompilerOptions.ENABLED);
				result.put(CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod, CompilerOptions.ENABLED);
			}
		} else if (javac.getDeprecation()) {
			result.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.WARNING);
			result.put(CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode, CompilerOptions.ENABLED);
			result.put(CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod, CompilerOptions.ENABLED);
		} else {
			result.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.IGNORE);
			result.put(CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode, CompilerOptions.DISABLED);
			result.put(CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod, CompilerOptions.DISABLED);
		}
		
		if (javac.getEncoding() != null) result.put(CompilerOptions.OPTION_Encoding, javac.getEncoding());
		
		return result;
	}
	
	/**
	 * Returns the compiler options for the given compiler options file.
	 * 
	 * If fileName is null or empty no file is read.
	 * 
	 * @param fileName The compiler options file. Might be null or empty string.
	 * @return the map with the compiler options.
	 */
	private static final Map<String, String> getFileCompilerOptions(String fileName) {
		if (fileName != null && !fileName.isEmpty()) {
			try {
				File compilerOptionsFile = new File(fileName);
				if (compilerOptionsFile.exists() && compilerOptionsFile.isFile()) {
					Map<String, String> compilerOptionsMap = propertiesAsMap(compilerOptionsFile);
					compilerOptionsMap = convertPreferences(compilerOptionsMap);
					return compilerOptionsMap;
				}
			} catch (Exception e) {
				throw new BuildException(String.format("Could not read compiler options file '%s'.\nReason: '%s'", fileName, e.getMessage()));
			}
		}
		return null;
	}
	
	private static Map<String, String> propertiesAsMap(File propertiesFile) {
		Map<String, String> result = new HashMap<String, String>();
		try {
			try (FileInputStream fis = new FileInputStream(propertiesFile)) {
				Properties properties = new Properties();
				properties.load(fis);
				
				for (Map.Entry<Object, Object> entry : properties.entrySet()) {
					result.put((String) entry.getKey(), (String) entry.getValue());
				}
				return result;
			}
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}
	
	/**
	 * This function alters the supplied options so exported preferences
	 * containing jdt compiler settings will be altered while removing the
	 * preference related prefix.
	 * 
	 * @param options
	 *            The options currently used. Maybe an exported preferences
	 *            file. Not <code>null</code>.
	 * 
	 * @return The altered settings. Not <code>null</code>.
	 */
	private static final Map<String, String> convertPreferences(Map<String, String> options) {
		Map<String, String> result = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : options.entrySet()) {
			if (entry.getKey().startsWith(PREFS_INSTANCE)) {
				// this is an exported preferences key
				String key = entry.getKey().substring(PREFS_INSTANCE.length());
				if (key.startsWith(PREFS_JDTTYPE)) {
					// we've got a jdt related setting, so use it
					key = key.substring(PREFS_JDTTYPE.length());
					result.put(key, entry.getValue());
				}
			} else {
				// not recognized as a preferences key
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}
	
	private static final Map<String, String> mergeCompilerOptions(Map<String, String> a, Map<String, String> b, Map<String, String> c) {
		Map<String, String> result = new HashMap<String, String>();
		if (c != null) result.putAll(c);
		if (b != null) result.putAll(b);
		if (a != null) result.putAll(a);
		return result;
	}
}
