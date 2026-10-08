package com.owl2gendl.generator.objectpropaxiom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

@Component
public class AllDisjointObjectPropertiesGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.ALL_DISJOINT_OBJECT_PROPERTIES;
	}

	private static final int MAX_ATTEMPTS_PER_MEMBER = 8;

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// DisjointObjectProperties requires every member to be simple, AND every pair within the group
			// to satisfy the same clash rules the pairwise ObjectPropertyDisjointWith construct enforces
			// (e.g. not already EquivalentObjectProperty with another group member) - an n-ary disjointness
			// axiom implies pairwise disjointness for every pair, not just simplicity of each member alone.
			// Bounded attempts, then fall through to minting a fresh (guaranteed-clash-free) property, so a
			// pool low on eligible properties can't spin this loop forever.
			Set<OWLObjectProperty> group = new LinkedHashSet<>();
			while (group.size() < 3) {
				OWLObjectProperty candidate = null;
				for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_MEMBER; attempt++) {
					OWLObjectProperty pick = ctx.pools().objectProperties().pick(ctx.random());
					boolean eligible = ctx.constraintTracker().eligibleForSimpleRequired(pick)
							&& !group.contains(pick)
							&& group.stream().allMatch(existing -> ctx.constraintTracker()
									.canApply(ConstructId.OBJECT_PROPERTY_DISJOINT_WITH, pick, existing)
									&& ctx.constraintTracker().canDeclareDisjointObjectProperties(pick, existing));
					if (eligible) {
						candidate = pick;
						break;
					}
				}
				group.add(candidate != null ? candidate : ctx.pools().objectProperties().mintNew());
			}
			group.forEach(p -> ctx.constraintTracker().markSimpleRequired(p));
			for (OWLObjectProperty a : group) {
				for (OWLObjectProperty b : group) {
					if (!a.equals(b)) {
						ctx.constraintTracker().recordApplied(ConstructId.OBJECT_PROPERTY_DISJOINT_WITH, a, b);
					}
				}
			}
			axioms.add(ctx.dataFactory().getOWLDisjointObjectPropertiesAxiom(group));
		}
		return axioms;
	}
}
