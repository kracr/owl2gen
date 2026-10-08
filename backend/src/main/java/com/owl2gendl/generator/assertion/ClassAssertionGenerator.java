package com.owl2gendl.generator.assertion;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

@Component
public class ClassAssertionGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.CLASS_ASSERTION;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLNamedIndividual individual = ctx.pools().individuals().pick(ctx.random());
			OWLClass type = ctx.pools().classes().pick(ctx.random());
			axioms.add(ctx.dataFactory().getOWLClassAssertionAxiom(type, individual));
		}
		return axioms;
	}
}
