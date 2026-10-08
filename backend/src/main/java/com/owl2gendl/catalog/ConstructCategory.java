package com.owl2gendl.catalog;

public enum ConstructCategory {
	CLASS_EXPRESSION("Class Expressions & Enumerations"),
	CLASS_AXIOM("Class Axioms"),
	OBJECT_PROPERTY_AXIOM("Object Property Axioms"),
	OBJECT_PROPERTY_RESTRICTION("Object Property Restrictions"),
	DATA_PROPERTY_AXIOM("Data Property Axioms"),
	DATA_PROPERTY_RESTRICTION("Data Property Restrictions"),
	DATA_RANGE("Data Ranges"),
	ASSERTION("Assertions & Keys");

	private final String displayName;

	ConstructCategory(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
