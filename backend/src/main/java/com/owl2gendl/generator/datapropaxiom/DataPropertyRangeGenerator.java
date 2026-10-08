package com.owl2gendl.generator.datapropaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

@Component
public class DataPropertyRangeGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATA_PROPERTY_RANGE;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDataProperty property = ctx.pools().dataProperties().pick(ctx.random());
			axioms.add(ctx.dataFactory().getOWLDataPropertyRangeAxiom(property, ctx.dataFactory().getStringOWLDatatype()));
		}
		return axioms;
	}
}
