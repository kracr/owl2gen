package com.owl2gendl.generator.assertion;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.ConstraintedPick;
import com.owl2gendl.pool.IndividualPool;

@Component
public class SameIndividualGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.SAME_INDIVIDUAL;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		IndividualPool individuals = ctx.pools().individuals();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// sameAs is transitive: merging a/b's same-as groups must not force an already-asserted
			// DifferentIndividuals pair to become equal.
			OWLNamedIndividual[] pair = ConstraintedPick.find(
					() -> {
						OWLNamedIndividual a = individuals.pick(ctx.random());
						OWLNamedIndividual b = individuals.pickDifferentFrom(ctx.random(), a);
						return new OWLNamedIndividual[] {a, b};
					},
					p -> ctx.constraintTracker().canAssertSame(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without transitively contradicting an existing Different axiom
			}
			axioms.add(ctx.dataFactory().getOWLSameIndividualAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordSame(pair[0], pair[1]);
		}
		return axioms;
	}
}
