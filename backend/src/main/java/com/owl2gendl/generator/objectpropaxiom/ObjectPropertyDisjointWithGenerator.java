package com.owl2gendl.generator.objectpropaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.ConstraintedPick;
import com.owl2gendl.pool.ObjectPropertyPool;

@Component
public class ObjectPropertyDisjointWithGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_PROPERTY_DISJOINT_WITH;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		ObjectPropertyPool properties = ctx.pools().objectProperties();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// DisjointObjectProperties requires both members to be simple (OWL2 DL structural restriction),
			// same requirement as the n-ary AllDisjointObjectProperties construct.
			OWLObjectProperty[] pair = ConstraintedPick.find(
					() -> {
						OWLObjectProperty a = properties.pick(ctx.random());
						OWLObjectProperty b = properties.pickDifferentFrom(ctx.random(), a);
						return new OWLObjectProperty[] {a, b};
					},
					p -> ctx.constraintTracker().eligibleForSimpleRequired(p[0])
							&& ctx.constraintTracker().eligibleForSimpleRequired(p[1])
							&& ctx.constraintTracker().canApply(id(), p[0], p[1])
							&& ctx.constraintTracker().canDeclareDisjointObjectProperties(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without clashing with an existing (possibly transitive) EquivalentObjectProperty axiom
			}
			axioms.add(ctx.dataFactory().getOWLDisjointObjectPropertiesAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordApplied(id(), pair[0], pair[1]);
			ctx.constraintTracker().markSimpleRequired(pair[0]);
			ctx.constraintTracker().markSimpleRequired(pair[1]);
		}
		return axioms;
	}
}
