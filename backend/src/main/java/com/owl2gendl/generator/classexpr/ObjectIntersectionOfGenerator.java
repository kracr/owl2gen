package com.owl2gendl.generator.classexpr;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/** {@code subject ≡ operandA ⊓ operandB}. */
@Component
public class ObjectIntersectionOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_INTERSECTION_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().mintNew();
			OWLClassExpression a = ctx.nestingStrategy().buildClassFiller(ctx, 0);
			OWLClassExpression b = ctx.nestingStrategy().buildDistinctClassFiller(ctx, 0, a);
			OWLClassExpression intersection = ctx.dataFactory().getOWLObjectIntersectionOf(a, b);
			axioms.add(ctx.dataFactory().getOWLEquivalentClassesAxiom(subject, intersection));
		}
		return axioms;
	}
}
