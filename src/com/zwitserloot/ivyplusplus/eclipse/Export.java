package com.zwitserloot.ivyplusplus.eclipse;

public class Export {
	private String org;
	private String name;
	
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
	
	@Override public String toString() {
		return "Export [org=" + org + ", name=" + name + "]";
	}
	
	@Override public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((org == null) ? 0 : org.hashCode());
		return result;
	}
	
	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Export other = (Export) obj;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		if (org == null) {
			if (other.org != null) return false;
		} else if (!org.equals(other.org)) return false;
		return true;
	}
}
