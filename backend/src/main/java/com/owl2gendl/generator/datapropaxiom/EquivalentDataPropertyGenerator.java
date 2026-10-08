package com.owl2gendl.generator.datapropaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.ConstraintedPick;
import com.owl2gendl.pool.DataPropertyPool;

@Component
public class EquivalentDataPropertyGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.EQUIVALENT_DATA_PROPERTY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		DataPropertyPool properties = ctx.pools().dataProperties();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDataProperty[] pair = ConstraintedPick.find(
					() -> {
						OWLDataProperty a = properties.pick(ctx.random());
						OWLDataProperty b = properties.pickDifferentFrom(ctx.random(), a);
						return new OWLDataProperty[] {a, b};
					},
					p -> ctx.constraintTracker().canApply(id(), p[0], p[1])
							&& ctx.constraintTracker().canMergeEquivalentDataProperties(p[0], p[1]));
			if (pair == null) {
				continue; // no pair currently eligible without clashing with an existing DataPropertyDisjointWith axiom
			}
			axioms.add(ctx.dataFactory().getOWLEquivalentDataPropertiesAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordApplied(id(), pair[0], pair[1]);
			ctx.constraintTracker().mergeEquivalentDataProperties(pair[0], pair[1]);
		}
		return axioms;
	}
}
