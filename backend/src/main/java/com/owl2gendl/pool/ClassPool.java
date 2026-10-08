package com.owl2gendl.pool;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.PrefixManager;

public class ClassPool extends EntityPool<OWLClass> {

	private final OWLDataFactory factory;
	private final PrefixManager prefixManager;

	public ClassPool(OWLDataFactory factory, PrefixManager prefixManager) {
		this.factory = factory;
		this.prefixManager = prefixManager;
	}

	@Override
	protected OWLClass createNamed(String name) {
		return factory.getOWLClass(name, prefixManager);
	}

	@Override
	protected String namePrefix() {
		return ":Class";
	}
}
