package com.owl2gendl.generator.datapropaxiom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

@Component
public class AllDisjointDataPropertiesGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.ALL_DISJOINT_DATA_PROPERTIES;
	}

	private static final int MAX_ATTEMPTS_PER_MEMBER = 8;

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// Every pair within the group must satisfy the same clash rule the pairwise
			// DataPropertyDisjointWith construct enforces (not already EquivalentDataProperty with another
			// group member) - an n-ary disjointness axiom implies pairwise disjointness for every pair.
			Set<OWLDataProperty> group = new LinkedHashSet<>();
			while (group.size() < 3) {
				OWLDataProperty candidate = null;
				for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_MEMBER; attempt++) {
					OWLDataProperty pick = ctx.pools().dataProperties().pick(ctx.random());
					boolean eligible = !group.contains(pick) && group.stream().allMatch(existing -> ctx.constraintTracker()
							.canApply(ConstructId.DATA_PROPERTY_DISJOINT_WITH, pick, existing)
							&& ctx.constraintTracker().canDeclareDisjointDataProperties(pick, existing));
					if (eligible) {
						candidate = pick;
						break;
					}
				}
				group.add(candidate != null ? candidate : ctx.pools().dataProperties().mintNew());
			}
			for (OWLDataProperty a : group) {
				for (OWLDataProperty b : group) {
					if (!a.equals(b)) {
						ctx.constraintTracker().recordApplied(ConstructId.DATA_PROPERTY_DISJOINT_WITH, a, b);
					}
				}
			}
			axioms.add(ctx.dataFactory().getOWLDisjointDataPropertiesAxiom(group));
		}
		return axioms;
	}
}
