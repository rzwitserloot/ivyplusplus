package com.zwitserloot.ivyplusplus;

import java.io.File;

public class AnnotationProcessorEntry {
	private String processorClass;
	private File jar;
	
	public void setProcessor(String proc) {
		this.processorClass = proc;
	}
	
	public void setJar(File jar) {
		this.jar = jar;
	}
	
	public String getProcessor() {
		return processorClass;
	}
	
	public File getJar() {
		return jar;
	}
}