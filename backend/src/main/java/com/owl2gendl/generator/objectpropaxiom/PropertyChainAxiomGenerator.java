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
 * {@code propertyA ∘ propertyB ⊑ superProperty}. The super-property becomes non-simple by definition (it is
 * implied by a chain), so it must not already be required to stay simple elsewhere (cardinality
 * restrictions, ObjectHasSelf, Irreflexive, Asymmetric, Functional, DisjointObjectProperties). OWL 2 DL also
 * requires the overall chain hierarchy to be "regular" (acyclic under a strict order) - both factors must be
 * addable as predecessors of the super-property without closing a cycle with any chain/sub-property edge
 * recorded so far, or the OWL API's own profile checker rejects the whole ontology.
 */
@Component
public class PropertyChainAxiomGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.PROPERTY_CHAIN_AXIOM;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		ObjectPropertyPool properties = ctx.pools().objectProperties();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLObjectProperty[] picked = ConstraintedPick.find(
					() -> {
						OWLObjectProperty first = properties.pick(ctx.random());
						OWLObjectProperty second = properties.pick(ctx.random());
						OWLObjectProperty superProperty = properties.pickDifferentFrom(ctx.random(), first);
						return new OWLObjectProperty[] {first, second, superProperty};
					},
					p -> !p[1].equals(p[2])
							&& ctx.constraintTracker().eligibleForNonSimple(p[2])
							&& ctx.constraintTracker().canAddChainOrder(p[0], p[2])
							&& ctx.constraintTracker().canAddChainOrder(p[1], p[2]));
			if (picked == null) {
				continue; // no factor/super-property combination keeps both simplicity and chain regularity intact
			}
			OWLObjectProperty first = picked[0];
			OWLObjectProperty second = picked[1];
			OWLObjectProperty superProperty = picked[2];
			axioms.add(ctx.dataFactory().getOWLSubPropertyChainOfAxiom(List.of(first, second), superProperty));
			ctx.constraintTracker().markNonSimple(superProperty);
			ctx.constraintTracker().recordChainOrder(first, superProperty);
			ctx.constraintTracker().recordChainOrder(second, superProperty);
		}
		return axioms;
	}
}
