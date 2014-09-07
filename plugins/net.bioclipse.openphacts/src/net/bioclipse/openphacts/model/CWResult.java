package net.bioclipse.openphacts.model;

public class CWResult {
	
	String name;
	String cwid;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCwid() {
		return cwid;
	}
	public String getURI() {
		return "http://www.conceptwiki.org/concept/" + cwid;
	}
	public void setCwid(String cwid) {
		this.cwid = cwid;
	}
	public CWResult(String name, String cwid) {
		super();
		this.name = name;
		this.cwid = cwid;
	}
	@Override
	public String toString() {
		return "CWResult [name=" + name + ", cwid=" + cwid + "]";
	}
	
	
}
