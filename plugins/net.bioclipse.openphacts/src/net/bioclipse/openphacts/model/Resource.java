package net.bioclipse.openphacts.model;

public class Resource {
	
	String name;
	String uri;

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getURI() {
		return "http://www.conceptwiki.org/concept/" + uri;
	}
	public void setURI(String uri) {
		this.uri = uri;
	}
	public Resource(String name, String uri) {
		super();
		this.name = name;
		this.uri = uri;
	}
	@Override
	public String toString() {
		return "Result [name=" + name + ", uri=" + uri + "]";
	}
	
	
}
