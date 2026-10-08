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
import com.owl2gendl.generator.support.ConstraintedPick;

@Component
public class ObjectMaxCardinalityGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_MAX_CARDINALITY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			// No Functional check needed: MaxCardinality(n>=1) is always compatible with (or implied by) Functional.
			OWLObjectProperty property = ConstraintedPick.find(
					() -> ctx.pools().objectProperties().pick(ctx.random()),
					p -> ctx.constraintTracker().eligibleForSimpleRequired(p));
			if (property == null) {
				continue; // every currently-eligible property is already non-simple
			}
			int cardinality = 1 + ctx.random().nextInt(3);
			OWLClassExpression restriction = ctx.dataFactory().getOWLObjectMaxCardinality(cardinality, property);
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(subject, restriction));
			ctx.constraintTracker().markSimpleRequired(property);
		}
		return axioms;
	}
}
