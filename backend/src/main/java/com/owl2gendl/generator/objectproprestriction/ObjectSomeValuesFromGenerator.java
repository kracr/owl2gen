package com.owl2gendl.generator.objectproprestriction;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/**
 * Generates existential restriction axioms of the shape {@code Subject ⊑ ∃property.Filler} — e.g.
 * {@code Faculty ⊑ ∃worksFor.College}.
 */
@Component
public class ObjectSomeValuesFromGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_SOME_VALUES_FROM;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			OWLObjectProperty property = ctx.pools().objectProperties().pick(ctx.random());
			OWLClassExpression filler = ctx.nestingStrategy().buildClassFiller(ctx, 0);
			OWLClassExpression restriction = ctx.dataFactory().getOWLObjectSomeValuesFrom(property, filler);
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(subject, restriction));
		}
		return axioms;
	}
}
