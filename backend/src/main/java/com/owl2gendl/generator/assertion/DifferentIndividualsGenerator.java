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
public class DifferentIndividualsGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DIFFERENT_INDIVIDUALS;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		IndividualPool individuals = ctx.pools().individuals();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// Must not contradict an already-asserted Same pairing, including transitively (a~b, a~c means
			// b and c are also forced equal, so Different(b,c) would be a contradiction).
			OWLNamedIndividual[] pair = ConstraintedPick.find(
					() -> {
						OWLNamedIndividual a = individuals.pick(ctx.random());
						OWLNamedIndividual b = individuals.pickDifferentFrom(ctx.random(), a);
						return new OWLNamedIndividual[] {a, b};
					},
					p -> ctx.constraintTracker().canAssertDifferent(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without contradicting an existing (possibly transitive) Same axiom
			}
			axioms.add(ctx.dataFactory().getOWLDifferentIndividualsAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordDifferent(pair[0], pair[1]);
		}
		return axioms;
	}
}
