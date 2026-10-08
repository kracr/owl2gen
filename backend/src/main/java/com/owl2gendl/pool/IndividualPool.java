package com.owl2gendl.pool;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.PrefixManager;

public class IndividualPool extends EntityPool<OWLNamedIndividual> {

	private final OWLDataFactory factory;
	private final PrefixManager prefixManager;

	public IndividualPool(OWLDataFactory factory, PrefixManager prefixManager) {
		this.factory = factory;
		this.prefixManager = prefixManager;
	}

	@Override
	protected OWLNamedIndividual createNamed(String name) {
		return factory.getOWLNamedIndividual(name, prefixManager);
	}

	@Override
	protected String namePrefix() {
		return ":individual";
	}
}
