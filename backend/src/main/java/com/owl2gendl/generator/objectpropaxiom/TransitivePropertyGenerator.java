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
public class TransitivePropertyGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.TRANSITIVE_PROPERTY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLObjectProperty property = ConstraintedPick.find(
					() -> ctx.pools().objectProperties().pick(ctx.random()),
					p -> ctx.constraintTracker().eligibleForNonSimple(p));
			if (property == null) {
				continue; // every currently-eligible property is already required to stay simple elsewhere
			}
			axioms.add(ctx.dataFactory().getOWLTransitiveObjectPropertyAxiom(property));
			ctx.constraintTracker().markNonSimple(property);
		}
		return axioms;
	}
}
