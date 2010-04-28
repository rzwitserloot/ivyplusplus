package com.zwitserloot.ivyplusplus.eclipse;

import java.io.File;

import lombok.Data;

@Data
public class Srcdir {
	private File dir;
	private boolean optional = false;
}
