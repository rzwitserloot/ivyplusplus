package com.zwitserloot.ivyplusplus.eclipse;

import java.io.File;

public class Lib {
	File location;
	boolean export = false;
	
	public File getLocation() {
		return location;
	}
	
	public void setLocation(File location) {
		this.location = location;
	}
	
	public boolean isExport() {
		return export;
	}
	
	public void setExport(boolean export) {
		this.export = export;
	}
	
	@Override public String toString() {
		return "Lib [location=" + location + ", export=" + export + "]";
	}

	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (export ? 1231 : 1237);
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		return result;
	}
	
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Lib other = (Lib) obj;
		if (export != other.export) return false;
		if (location == null) {
			if (other.location != null) return false;
		} else if (!location.equals(other.location)) return false;
		return true;
	}
}
