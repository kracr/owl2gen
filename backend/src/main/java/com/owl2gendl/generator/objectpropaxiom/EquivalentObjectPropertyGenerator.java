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

/**
 * {@code EquivalentObjectProperties(a, b)} semantically implies both {@code a ⊑ b} and {@code b ⊑ a}, so it
 * participates in the same chain-order regularity graph as {@code SubObjectPropertyOf} and
 * {@code PropertyChainAxiom}: it must not close a cycle with an existing chain conclusion (e.g. a property
 * already recorded as coming before some other property can't be declared equivalent to something that
 * property already needs to come before).
 */
@Component
public class EquivalentObjectPropertyGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.EQUIVALENT_OBJECT_PROPERTY;
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
					p -> ctx.constraintTracker().canApply(id(), p[0], p[1])
							&& ctx.constraintTracker().canAddChainOrder(p[0], p[1])
							&& ctx.constraintTracker().canAddChainOrder(p[1], p[0])
							&& ctx.constraintTracker().canMergeEquivalentObjectProperties(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without clashing with ObjectPropertyDisjointWith or closing a chain cycle
			}
			axioms.add(ctx.dataFactory().getOWLEquivalentObjectPropertiesAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordApplied(id(), pair[0], pair[1]);
			ctx.constraintTracker().recordChainOrder(pair[0], pair[1]);
			ctx.constraintTracker().recordChainOrder(pair[1], pair[0]);
			ctx.constraintTracker().mergeEquivalentObjectProperties(pair[0], pair[1]);
		}
		return axioms;
	}
}
