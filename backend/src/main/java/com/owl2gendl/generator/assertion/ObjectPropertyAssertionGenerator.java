package com.owl2gendl.generator.assertion;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.pool.IndividualPool;

@Component
public class ObjectPropertyAssertionGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_PROPERTY_ASSERTION;
	}

	private static final int MAX_ATTEMPTS = 8;

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		IndividualPool individuals = ctx.pools().individuals();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLObjectProperty property = null;
			OWLNamedIndividual subject = null;
			OWLNamedIndividual object = null;
			for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
				OWLObjectProperty p = ctx.pools().objectProperties().pick(ctx.random());
				OWLNamedIndividual s = individuals.pick(ctx.random());
				OWLNamedIndividual o = individuals.pickDifferentFrom(ctx.random(), s);
				if (ctx.constraintTracker().canApply(id(), p, s, o)) {
					property = p;
					subject = s;
					object = o;
					break;
				}
			}
			if (property == null) {
				continue; // every recently-tried triple already has a NegativeObjectPropertyAssertion on it
			}
			axioms.add(ctx.dataFactory().getOWLObjectPropertyAssertionAxiom(property, subject, object));
			ctx.constraintTracker().recordApplied(id(), property, subject, object);
		}
		return axioms;
	}
}
