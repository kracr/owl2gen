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

@Component
public class SubObjectPropertyOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.SUB_OBJECT_PROPERTY_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		ObjectPropertyPool properties = ctx.pools().objectProperties();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// Shares the chain-order graph with PropertyChainAxiomGenerator - a plain sub-property edge can
			// combine with existing chain edges to close a cycle just as easily as two chain edges can, and
			// the OWL API's regularity check rejects the whole ontology either way.
			OWLObjectProperty[] pair = ConstraintedPick.find(
					() -> {
						OWLObjectProperty sub = properties.pick(ctx.random());
						OWLObjectProperty sup = properties.pickDifferentFrom(ctx.random(), sub);
						return new OWLObjectProperty[] {sub, sup};
					},
					p -> ctx.constraintTracker().canAddChainOrder(p[0], p[1]));
			if (pair == null) {
				continue; // no sub/super-property pair currently keeps the chain hierarchy regular
			}
			axioms.add(ctx.dataFactory().getOWLSubObjectPropertyOfAxiom(pair[0], pair[1]));
			ctx.constraintTracker().recordChainOrder(pair[0], pair[1]);
		}
		return axioms;
	}
}
