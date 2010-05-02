package com.zwitserloot.ivyplusplus.eclipse;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import lombok.Cleanup;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.types.Resource;

public class Settings {
	private TreeMap<String, String> properties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	private List<Object> inputs = new ArrayList<Object>();
	
	public void addText(String text) {
		inputs.add(text);
	}
	
	private void loadProps(InputStream in, boolean overwrite) throws IOException {
		Properties p = new Properties();
		p.load(in);
		for (Map.Entry<Object, Object> e : p.entrySet()) {
			String key = (String) e.getKey();
			String value = (String) e.getValue();
			if (overwrite || !properties.containsKey(key)) properties.put(key, value);
		}
	}
	
	public void add(Resource resource) {
		inputs.add(resource);
	}
	
	private static final Map<String, String> PROPERTY_NAME_MAP;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("eclipse.preferences.version", null);
		map.put("cleanup.", "org.eclipse.jdt.ui");
		map.put("cleanup_", "org.eclipse.jdt.ui");
		map.put("sp_cleanup.", "org.eclipse.jdt.ui");
		map.put("formatter_", "org.eclipse.jdt.ui");
		map.put("editor_save_participant_org.eclipse.jdt.ui.", "org.eclipse.jdt.ui");
		map.put("org.eclipse.jdt.ui.", "org.eclipse.jdt.ui");
		map.put("encoding/", "org.eclipse.core.resources");
		map.put("org.eclipse.jdt.core.", "org.eclipse.jdt.core");
		map.put("line.separator", "org.eclipse.core.runtime");
		map.put("org.eclipse.jdt.launching.", "org.eclipse.jdt.launching");
		map.put("org.eclipse.jdt.apt.", "org.eclipse.jdt.apt.core");
		map.put("org.eclipse.ltk.core.refactoring.", "org.eclipse.ltk.core.refactoring");
		
		PROPERTY_NAME_MAP = Collections.unmodifiableMap(map);
	}
	
	public void execute(File todir, Location location, String source) {
		boolean is5 = "1.5".equals(source);
		boolean is6 = "1.6".equals(source);
		boolean is7 = "1.7".equals(source);
		
		todir = new File(todir, ".settings");
		
		boolean normalExit = false;
		for (Object input : inputs) {
			if (input == null) continue;
			if (input instanceof String) {
				try {
					@Cleanup InputStream in = new ByteArrayInputStream(((String) input).getBytes("ISO-8859-1"));
					loadProps(in, true);
				} catch (IOException e) {
					throw new BuildException(e, location);
				}
			} else if (input instanceof Resource) {
				try {
					@Cleanup InputStream in = ((Resource) input).getInputStream();
					loadProps(in, false);
				} catch (IOException e) {
					throw new BuildException(e, location);
				}
			} else {
				assert false: "A non-string, non-resource showed up";
			}
		}
		
		if (!properties.containsKey("org.eclipse.jdt.core.compiler.processAnnotations")) {
			if (is5) properties.put("org.eclipse.jdt.core.compiler.processAnnotations", "disabled");
			if (is6 || is7) properties.put("org.eclipse.jdt.core.compiler.processAnnotations", "enabled");
		}
		
		if (!properties.containsKey("org.eclipse.jdt.core.compiler.source")) {
			if (is5 || is6 || is7) properties.put("org.eclipse.jdt.core.compiler.source", source);
		}
		
		if (!properties.containsKey("org.eclipse.jdt.core.compiler.compliance")) {
			if (is5 || is6 || is7) properties.put("org.eclipse.jdt.core.compiler.compliance", source);
		}
		
		if (!properties.containsKey("org.eclipse.jdt.core.compiler.codegen.targetPlatform")) {
			if (is5 || is6 || is7) properties.put("org.eclipse.jdt.core.compiler.codegen.targetPlatform", source);
		}
		
		try {
			for (Map.Entry<String, String> e : properties.entrySet()) {
				String key = e.getKey();
				String value = e.getValue();
				
				boolean handled = false;
				for (Map.Entry<String, String> f : PROPERTY_NAME_MAP.entrySet()) {
					if (key.startsWith(f.getKey())) {
						handled = true;
						addToFile(f.getValue(), key, value, todir, location);
						break;
					}
				}
				
				if (!handled) {
					throw new BuildException("Unknown eclipse property: " + key, location);
				}
			}
			normalExit = true;
		} catch (IOException e) {
			throw new BuildException(e);
		} finally {
			Throwable stored = null;
			for (FileOutputStream out : createdFiles.values()) {
				try {
					out.close();
				} catch (Throwable t) {
					if (stored != null) stored = t;
				}
			}
			if (normalExit && stored != null) {
				if (stored instanceof BuildException) throw (BuildException) stored;
				throw new BuildException(stored, location);
			}
		}
	}
	
	private Map<String, FileOutputStream> createdFiles = new HashMap<String, FileOutputStream>();
	
	private void addToFile(String fileName, String key, String value, File todir, Location location) throws IOException {
		if (!todir.exists()) {
			if (!todir.mkdirs()) throw new BuildException("Can't create directory: " + todir.getAbsolutePath(), location);
		}
		if (!todir.isDirectory()) throw new BuildException("Not a directory: " + todir.getAbsolutePath(), location);
		FileOutputStream out;
		if (!createdFiles.containsKey(fileName)) {
			File f = new File(todir, fileName + ".prefs");
			if (f.exists()) f.delete();
			createdFiles.put(fileName, out = new FileOutputStream(f));
			writePreamble(out);
		} else {
			out = createdFiles.get(fileName);
		}
		
		out.write(escapeKey(key).getBytes("ISO-8859-1"));
		out.write('=');
		out.write(escapeValue(value).getBytes("ISO-8859-1"));
		out.write(System.getProperty("line.separator", "\n").getBytes("ISO-8859-1"));
	}
	
	private String escapeKey(String key) {
		StringBuilder sb = new StringBuilder();
		for (char c : escapeValue(key).toCharArray()) {
			if (c == ':' || c == '=' || c == ' ') {
				sb.append('\\');
				sb.append(c);
			} else sb.append(c);
		}
		return sb.toString();
	}
	
	private String escapeValue(String key) {
		StringBuilder sb = new StringBuilder();
		for (char c : key.toCharArray()) {
			if (c == '\n') sb.append("\\n");
			else if (c == '\r') sb.append("\\r");
			else if (c == '\t') sb.append("\\t");
			else if (c == '\f') sb.append("\\f");
			else if (c == '\\') sb.append("\\\\");
			else if (c < 32 || c > 126) {
				sb.append('\\').append('u').append(String.format("%04X", (int)c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	private void writePreamble(OutputStream out) throws IOException {
		out.write(("#" + new Date().toString()).getBytes("ISO-8859-1"));
		out.write(System.getProperty("line.separator", "\n").getBytes("ISO-8859-1"));
		out.write("eclipse.preferences.version=1".getBytes("ISO-8859-1"));
		out.write(System.getProperty("line.separator", "\n").getBytes("ISO-8859-1"));
	}
}
