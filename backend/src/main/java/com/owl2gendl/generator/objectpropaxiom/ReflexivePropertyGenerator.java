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

@Component
public class ReflexivePropertyGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.REFLEXIVE_PROPERTY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLObjectProperty property = ConstraintedPick.find(
					() -> ctx.pools().objectProperties().pick(ctx.random()),
					p -> ctx.constraintTracker().canApply(id(), p));
			if (property == null) {
				continue; // no property currently eligible without clashing with an existing IrreflexiveProperty axiom
			}
			axioms.add(ctx.dataFactory().getOWLReflexiveObjectPropertyAxiom(property));
			ctx.constraintTracker().recordApplied(id(), property);
		}
		return axioms;
	}
}
