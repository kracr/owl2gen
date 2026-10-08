package com.owl2gendl.generator.assertion;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/** A class is uniquely identified by one object property and one data property. */
@Component
public class HasKeyGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.HAS_KEY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			OWLObjectProperty objectProperty = ctx.pools().objectProperties().pick(ctx.random());
			OWLDataProperty dataProperty = ctx.pools().dataProperties().pick(ctx.random());
			axioms.add(ctx.dataFactory().getOWLHasKeyAxiom(subject, objectProperty, dataProperty));
		}
		return axioms;
	}
}
