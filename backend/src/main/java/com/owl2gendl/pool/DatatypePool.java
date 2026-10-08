package com.owl2gendl.pool;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.PrefixManager;

/** Pool of custom (non-built-in) datatypes, minted to anchor generated data-range expressions. */
public class DatatypePool extends EntityPool<OWLDatatype> {

	private final OWLDataFactory factory;
	private final PrefixManager prefixManager;

	public DatatypePool(OWLDataFactory factory, PrefixManager prefixManager) {
		this.factory = factory;
		this.prefixManager = prefixManager;
	}

	@Override
	protected OWLDatatype createNamed(String name) {
		return factory.getOWLDatatype(name, prefixManager);
	}

	@Override
	protected String namePrefix() {
		return ":datatype";
	}
}
