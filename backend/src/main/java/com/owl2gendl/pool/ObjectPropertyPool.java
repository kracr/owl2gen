package com.owl2gendl.pool;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.PrefixManager;

public class ObjectPropertyPool extends EntityPool<OWLObjectProperty> {

	private final OWLDataFactory factory;
	private final PrefixManager prefixManager;

	public ObjectPropertyPool(OWLDataFactory factory, PrefixManager prefixManager) {
		this.factory = factory;
		this.prefixManager = prefixManager;
	}

	@Override
	protected OWLObjectProperty createNamed(String name) {
		return factory.getOWLObjectProperty(name, prefixManager);
	}

	@Override
	protected String namePrefix() {
		return ":objectProperty";
	}
}
