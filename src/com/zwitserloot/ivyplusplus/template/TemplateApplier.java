package com.zwitserloot.ivyplusplus.template;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class TemplateApplier {
	private static final class IfStackEntry {
		final String key;
		final boolean value;
		final int pos;
		
		IfStackEntry(String key, int pos, boolean value) {
			this.key = key;
			this.value = value;
			this.pos = pos;
		}
	}
	
	@Override public String toString() {
		return map.toString();
	}
	
	private final Map<String, String> map = new HashMap<String, String>();
	
	public String applyStream(InputStream in) throws IOException {
		return applyStream(in, "UTF-8");
	}
	
	public String applyStream(InputStream in, String charset) throws IOException {
		byte[] data;
		try {
			data = read(in);
		} finally {
			in.close();
		}
		return applyTemplate(new String(data, charset));
	}
	
	public String applyFile(File in) throws IOException {
		return applyFile(in, "UTF-8");
	}
	
	public String applyFile(File in, String charset) throws IOException {
		FileInputStream fis = new FileInputStream(in);
		byte[] data;
		try {
			data = read(fis);
		} finally {
			fis.close();
		}
		
		return applyTemplate(new String(data, charset));
	}
	
	public String applyResource(Class<?> context, String resource) throws IOException {
		return applyResource(context, resource, "UTF-8");
	}
	
	public String applyResource(Class<?> context, String resource, String charset) throws IOException {
		InputStream is = context.getResourceAsStream(resource);
		byte[] data;
		try {
			data = read(is);
		} finally {
			is.close();
		}
		return applyTemplate(new String(data, charset));
	}
	
	public void put(String key, String value) {
		this.map.put(key, value);
	}
	
	public String applyTemplate(String template) {
		StringBuilder sb = new StringBuilder();
		
		int pos = 0;
		
		Deque<IfStackEntry> ifStack = new ArrayDeque<IfStackEntry>();
		boolean suppress = false;
		
		while (pos < template.length()) {
			int idx = template.indexOf("{{", pos);
			if (idx == -1) {
				if (!suppress) sb.append(template.substring(pos));
				break;
			}
			
			if (!suppress) sb.append(template.substring(pos, idx));
			
			int braceCount = 2;
			while (idx + braceCount < template.length()) {
				if (template.charAt(idx + braceCount) != '{') break;
				braceCount++;
			}
			
			if (braceCount > 2) {
				pos = idx + braceCount;
				while (braceCount > 1) {
					if (!suppress) sb.append("{");
					braceCount--;
				}
				continue;
			}
			
			int endIdx = template.indexOf("}}", idx);
			if (endIdx == -1) {
				if (!suppress) sb.append(template.substring(idx));
				break;
			}
			
			String command = template.substring(idx + 2, endIdx);
			
			boolean removePrefixWhitespace = false;
			pos = endIdx + 2;
			if (template.length() == endIdx + 2) {
				removePrefixWhitespace = true;
			} else if (template.length() > endIdx + 2 && template.charAt(endIdx + 2) == '\n') {
				removePrefixWhitespace = true;
				pos = endIdx + 3;
			} else if (template.length() > endIdx + 3 && template.charAt(endIdx + 2) == '\r' && template.charAt(endIdx + 2) == '\n') {
				removePrefixWhitespace = true;
				pos = endIdx + 4;
			}
			
			StringBuilder prefixWhitespace = new StringBuilder();
			
			if (removePrefixWhitespace) while (sb.length() > 0) {
				char ws = sb.charAt(sb.length() - 1);
				if (ws != ' ' && ws != '\t') break;
				prefixWhitespace.insert(0, ws);
				sb.setLength(sb.length() - 1);
			}
			
			if (command.startsWith("@")) {
				String key = command.substring(1).trim();
				if (!suppress) sb.append(map.containsKey(key) ? addWhitespacePrefix(prefixWhitespace.toString(), map.get(key)) : "");
			} else if (command.startsWith("if ")) {
				String key = command.substring(3).trim();
				boolean val = map.containsKey(key);
				ifStack.push(new IfStackEntry(key, idx, val));
				suppress |= !val;
			} else if (command.startsWith("end ")) {
				String key = command.substring(4).trim();
				if (ifStack.isEmpty()) throw new IllegalArgumentException(String.format("%s floating {{end %s}}", toLinePos(template, idx), key));
				ifStack.pop();
				suppress = false;
				for (IfStackEntry v : ifStack) suppress |= !v.value;
			} else {
				throw new IllegalArgumentException(String.format("%s Unknown command: %s", toLinePos(template, idx), command));
			}
		}
		
		if (!ifStack.isEmpty()) throw new IllegalArgumentException(String.format("%s {{if %s}} not closed", toLinePos(template, ifStack.peek().pos), ifStack.peek().key));
		
		return sb.toString();
	}
	
	private String addWhitespacePrefix(String prefix, String text) {
		String t = text.replace("\n", "\n" + prefix).trim();
		return prefix + t + (prefix.length() > 0 ? "\n" : "");
	}
	
	private static final byte[] read(InputStream in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		byte[] b = new byte[4096];
		while (true) {
			int r = in.read(b);
			if (r == -1) break;
			baos.write(b, 0, r);
		}
		
		return baos.toByteArray();
	}
	
	public static String toLinePos(String template, int idx) {
		int line = 0;
		int col = 0;
		for (int i = 0; i < idx; i++) {
			char c = template.charAt(i);
			col += c == '\t' ? 4 : 1;
			if (c == '\n') {
				line++;
				col = 0;
			}
		}
		
		return String.format("[%d, %d]", line + 1, col + 1);
	}
}
