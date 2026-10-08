package com.owl2gendl.generator.classaxiom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/** Pairwise disjointness over three or more classes in one axiom. */
@Component
public class AllDisjointClassesGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.ALL_DISJOINT_CLASSES;
	}

	private static final int MAX_ATTEMPTS_PER_MEMBER = 8;

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// Every pair within the group must satisfy the same clash rule the pairwise DisjointWith
			// construct enforces (not already EquivalentClasses with another group member) - an n-ary
			// disjointness axiom implies pairwise disjointness for every pair, not just for one at a time.
			Set<OWLClass> group = new LinkedHashSet<>();
			while (group.size() < 3) {
				OWLClass candidate = null;
				for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_MEMBER; attempt++) {
					OWLClass pick = ctx.pools().classes().pick(ctx.random());
					boolean eligible = !group.contains(pick) && group.stream().allMatch(existing ->
							ctx.constraintTracker().canApply(ConstructId.DISJOINT_WITH, pick, existing)
									&& ctx.constraintTracker().canDeclareDisjointClasses(pick, existing));
					if (eligible) {
						candidate = pick;
						break;
					}
				}
				group.add(candidate != null ? candidate : ctx.pools().classes().mintNew());
			}
			for (OWLClass a : group) {
				for (OWLClass b : group) {
					if (!a.equals(b)) {
						ctx.constraintTracker().recordApplied(ConstructId.DISJOINT_WITH, a, b);
					}
				}
			}
			axioms.add(ctx.dataFactory().getOWLDisjointClassesAxiom(group));
		}
		return axioms;
	}
}
