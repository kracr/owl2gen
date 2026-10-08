package com.owl2gendl.generator.classexpr;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/** {@code subject ≡ {individualA, individualB}} — an enumerated class of named individuals. */
@Component
public class ObjectOneOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_ONE_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().mintNew();
			OWLNamedIndividual a = ctx.pools().individuals().pick(ctx.random());
			OWLNamedIndividual b = ctx.pools().individuals().pickDifferentFrom(ctx.random(), a);
			OWLClassExpression oneOf = ctx.dataFactory().getOWLObjectOneOf(a, b);
			axioms.add(ctx.dataFactory().getOWLEquivalentClassesAxiom(subject, oneOf));
		}
		return axioms;
	}
}
