package com.owl2gendl.generator.classaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.ConstraintedPick;

@Component
public class EquivalentClassesGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.EQUIVALENT_CLASSES;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// EquivalentClasses is transitive: merging a/b's equivalence groups must not force an
			// already-Disjoint pair to become equal, checked across every cross-pair between the two groups.
			OWLClass[] pair = ConstraintedPick.find(
					() -> {
						OWLClass a = ctx.pools().classes().pick(ctx.random());
						OWLClass b = ctx.pools().classes().pickDifferentFrom(ctx.random(), a);
						return new OWLClass[] {a, b};
					},
					p -> ctx.constraintTracker().canApply(id(), p[0], p[1])
							&& ctx.constraintTracker().canMergeEquivalentClasses(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without clashing with an existing DisjointWith axiom
			}
			axioms.add(ctx.dataFactory().getOWLEquivalentClassesAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordApplied(id(), pair[0], pair[1]);
			ctx.constraintTracker().mergeEquivalentClasses(pair[0], pair[1]);
		}
		return axioms;
	}
}
