package com.owl2gendl.generator.datapropresrestriction;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

@Component
public class DataSomeValuesFromGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATA_SOME_VALUES_FROM;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			OWLDataProperty property = ctx.pools().dataProperties().pick(ctx.random());
			OWLClassExpression restriction = ctx.dataFactory().getOWLDataSomeValuesFrom(property, ctx.dataFactory().getStringOWLDatatype());
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(subject, restriction));
		}
		return axioms;
	}
}
