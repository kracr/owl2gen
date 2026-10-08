package com.owl2gendl.generator.objectpropaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.constraint.ConstraintTracker;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.ConstraintedPick;
import com.owl2gendl.pool.ObjectPropertyPool;

/**
 * A property can legally be named the inverse of more than one other property, but inverse is a well-defined
 * (involutive) operation - if {@code a} and {@code c} are both declared the inverse of {@code b}, then
 * {@code a} and {@code c} must denote the same relation. {@link ConstraintTracker#canDeclareInverse} guards
 * against this forcing an already-Disjoint pair to become equal.
 */
@Component
public class InverseOfPropertyGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.INVERSE_OF_PROPERTY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		ObjectPropertyPool properties = ctx.pools().objectProperties();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLObjectProperty[] pair = ConstraintedPick.find(
					() -> {
						OWLObjectProperty a = properties.pick(ctx.random());
						OWLObjectProperty b = properties.pickDifferentFrom(ctx.random(), a);
						return new OWLObjectProperty[] {a, b};
					},
					p -> ctx.constraintTracker().canDeclareInverse(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without transitively forcing a Disjoint pair to become equal
			}
			axioms.add(ctx.dataFactory().getOWLInverseObjectPropertiesAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordInverse(pair[0], pair[1]);
		}
		return axioms;
	}
}
