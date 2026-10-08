package com.owl2gendl.pool;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.PrefixManager;

public class DataPropertyPool extends EntityPool<OWLDataProperty> {

	private final OWLDataFactory factory;
	private final PrefixManager prefixManager;

	public DataPropertyPool(OWLDataFactory factory, PrefixManager prefixManager) {
		this.factory = factory;
		this.prefixManager = prefixManager;
	}

	@Override
	protected OWLDataProperty createNamed(String name) {
		return factory.getOWLDataProperty(name, prefixManager);
	}

	@Override
	protected String namePrefix() {
		return ":dataProperty";
	}
}
