package com.zwitserloot.ivyplusplus.eclipse;

import java.io.File;

import lombok.Data;

@Data
public class Lib {
	File location;
	boolean export = false;
}
