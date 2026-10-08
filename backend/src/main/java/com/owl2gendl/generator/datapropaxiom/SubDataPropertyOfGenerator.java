package com.owl2gendl.generator.datapropaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.pool.DataPropertyPool;

@Component
public class SubDataPropertyOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.SUB_DATA_PROPERTY_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		DataPropertyPool properties = ctx.pools().dataProperties();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDataProperty sub = properties.pick(ctx.random());
			OWLDataProperty sup = properties.pickDifferentFrom(ctx.random(), sub);
			axioms.add(ctx.dataFactory().getOWLSubDataPropertyOfAxiom(sub, sup));
		}
		return axioms;
	}
}
