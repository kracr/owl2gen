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

/** Exact qualified cardinality. */
@Component
public class ObjectQualifiedCardinalityGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_QUALIFIED_CARDINALITY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			int cardinality = 1 + ctx.random().nextInt(3);
			OWLObjectProperty property = ConstraintedPick.find(
					() -> ctx.pools().objectProperties().pick(ctx.random()),
					p -> ctx.constraintTracker().eligibleForSimpleRequired(p)
							&& (cardinality < 2 || !ctx.constraintTracker().isFunctional(p)));
			if (property == null) {
				continue; // no property currently eligible (non-simple, or Functional with cardinality >= 2)
			}
			OWLClassExpression filler = ctx.nestingStrategy().buildClassFiller(ctx, 0);
			OWLClassExpression restriction = ctx.dataFactory().getOWLObjectExactCardinality(cardinality, property, filler);
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(subject, restriction));
			ctx.constraintTracker().markSimpleRequired(property);
		}
		return axioms;
	}
}
