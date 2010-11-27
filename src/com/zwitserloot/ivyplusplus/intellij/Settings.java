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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import lombok.Cleanup;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.types.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Settings {
	private TreeMap<String, Element> components = new TreeMap<String, Element>(String.CASE_INSENSITIVE_ORDER);
	private List<Object> inputs = new ArrayList<Object>();
	private DocumentBuilderFactory factory;
	
	public void addText(String text) {
		inputs.add(text);
	}
	
	public static void main(String[] args) throws Exception {
		new Settings().loadXML(new FileInputStream(args[0]), true);
	}
	
	private void loadXML(InputStream in, boolean overwrite) throws IOException {
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(in);
			List<Element> components = new ArrayList<Element>();
			gatherComponents(doc.getDocumentElement(), components);
			for (Element component : components) {
				String name = component.getAttribute("name");
				if (overwrite || !this.components.containsKey(name)) this.components.put(name, component);
			}
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}
	
	private void gatherComponents(Element element, List<Element> list) {
		if ("component".equals(element.getNodeName())) {
			list.add(element);
			return;
		}
		NodeList nodeList = element.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node n = nodeList.item(i);
			if (n instanceof Element) gatherComponents((Element) nodeList.item(i), list);
		}
	}
	
	public void add(Resource resource) {
		inputs.add(resource);
	}
	
	private static final Map<String, String> COMPONENT_NAME_MAP;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("CompilerConfiguration", ".idea/compiler.xml");
		map.put("CodeStyleSettingsManager", ".idea/projectCodeStyle.xml");
		map.put("Encoding", ".idea/encodings.xml");
		map.put("Palette2", ".idea/uiDesigner.xml");
		map.put("VcsDirectoryMappings", ".idea/vcs.xml");
		map.put("ProjectModuleManager", ".idea/modules.xml");
		map.put("", ".idea/misc.xml");
		
		COMPONENT_NAME_MAP = Collections.unmodifiableMap(map);
	}
	
	public void execute(File todir, Location location, String source) {
		this.factory = DocumentBuilderFactory.newInstance();
		for (Object input : inputs) {
			if (input == null) continue;
			if (input instanceof String) {
				if (!((String)input).trim().isEmpty()) {
					try {
						@Cleanup InputStream in = new ByteArrayInputStream(((String) input).getBytes("ISO-8859-1"));
						loadXML(in, true);
					} catch (IOException e) {
						throw new BuildException(e, location);
					}
				}
			} else if (input instanceof Resource) {
				try {
					@Cleanup InputStream in = ((Resource) input).getInputStream();
					loadXML(in, false);
				} catch (IOException e) {
					throw new BuildException(e, location);
				}
			} else {
				assert false: "A non-string, non-resource showed up";
			}
		}
		
		Map<File, List<Element>> fileToComponents = new HashMap<File, List<Element>>();
		for (Map.Entry<String, Element> e : components.entrySet()) {
			String key = e.getKey();
			Element value = e.getValue();
			File file = null;
			
			for (Map.Entry<String, String> f : COMPONENT_NAME_MAP.entrySet()) {
				if (key.equalsIgnoreCase(f.getKey())) {
					file = new File(todir, f.getValue());
					break;
				}
			}
			
			if (file == null) file = new File(todir, COMPONENT_NAME_MAP.get(""));
			List<Element> elemList = fileToComponents.get(file);
			if (elemList == null) {
				elemList = new ArrayList<Element>();
				fileToComponents.put(file, elemList);
			}
			elemList.add(value);
		}
		
		try {
			for (Map.Entry<File, List<Element>> e : fileToComponents.entrySet()) {
				File key = e.getKey();
				List<Element> elemList = e.getValue();
				createFile(key, elemList, location);
			}
		} catch (IOException e) {
			throw new BuildException(e, location);
		}
	}
	
	private void createFile(File file, List<Element> elemList, Location location) throws IOException {
		try {
			file.getParentFile().mkdirs();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.newDocument();
			Element top = doc.createElement("project");
			top.setAttribute("version", "4");
			for (Element elem : elemList) {
				top.appendChild(doc.importNode(elem, true));
			}
			doc.appendChild(top);
			DOMSource src = new DOMSource(doc);
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(src, new StreamResult(file));
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerConfigurationException e) {
			throw new IOException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new IOException(e);
		} catch (TransformerException e) {
			throw new IOException(e);
		}
	}
}
