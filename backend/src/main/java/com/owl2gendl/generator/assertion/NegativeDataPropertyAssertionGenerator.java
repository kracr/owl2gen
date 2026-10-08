package com.owl2gendl.generator.assertion;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.Literals;

@Component
public class NegativeDataPropertyAssertionGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.NEGATIVE_DATA_PROPERTY_ASSERTION;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDataProperty property = ctx.pools().dataProperties().pick(ctx.random());
			OWLNamedIndividual subject = ctx.pools().individuals().pick(ctx.random());
			axioms.add(ctx.dataFactory().getOWLNegativeDataPropertyAssertionAxiom(property, subject, Literals.randomString(ctx.dataFactory(), ctx.random())));
		}
		return axioms;
	}
}
