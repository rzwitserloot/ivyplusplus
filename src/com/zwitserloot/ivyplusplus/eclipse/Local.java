package com.zwitserloot.ivyplusplus.eclipse;

public class Local {
	private String org;
	private String name;
	private String dir;
	
	public String getOrg() {
		return org;
	}
	
	public void setOrg(String org) {
		this.org = org;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDir() {
		return dir;
	}
	
	public void setDir(String dir) {
		this.dir = dir;
	}
	
	@Override public String toString() {
		return "Local [org=" + org + ", name=" + name + ", dir=" + dir + "]";
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dir == null) ? 0 : dir.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((org == null) ? 0 : org.hashCode());
		return result;
	}
	
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Local other = (Local) obj;
		if (dir == null) {
			if (other.dir != null) return false;
		} else if (!dir.equals(other.dir)) return false;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		if (org == null) {
			if (other.org != null) return false;
		} else if (!org.equals(other.org)) return false;
		return true;
	}
}
