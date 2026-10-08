package com.owl2gendl.generator.support;

import java.util.Random;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;

/** Generic literal values for data-property assertions and restrictions — domain-agnostic by design. */
public final class Literals {

	private Literals() {
	}

	public static OWLLiteral randomString(OWLDataFactory factory, Random random) {
		return factory.getOWLLiteral("value" + random.nextInt(1_000_000));
	}

	public static OWLLiteral randomInteger(OWLDataFactory factory, Random random) {
		return factory.getOWLLiteral(random.nextInt(1_000_000));
	}
}
