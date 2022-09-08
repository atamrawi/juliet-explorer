package com.ensoftcorp.open.juliet.parser;

import java.util.Objects;

public class JulietTestCase {

	private String identifier;
	
	private String link;
	
	private String cwe;
	
	private String flowVariantType;
	
	private String flowVariantCategory;
	
	public JulietTestCase(String identifier) {
		this.identifier = identifier;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getCwe() {
		return cwe;
	}

	public void setCwe(String cwe) {
		this.cwe = cwe;
	}

	public String getFlowVariantType() {
		return flowVariantType;
	}

	public void setFlowVariantType(String flowVariantType) {
		this.flowVariantType = flowVariantType;
	}

	public String getFlowVariantCategory() {
		return flowVariantCategory;
	}

	public void setFlowVariantCategory(String flowVariantCategory) {
		this.flowVariantCategory = flowVariantCategory;
	}

	@Override
	public int hashCode() {
		return Objects.hash(identifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JulietTestCase other = (JulietTestCase) obj;
		return Objects.equals(identifier, other.identifier);
	}

	@Override
	public String toString() {
		return "JulietTestCase [identifier=" + identifier + "]";
	}
	
}
