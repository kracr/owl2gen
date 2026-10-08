package com.owl2gendl.generator.objectpropaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

@Component
public class ObjectPropertyDomainGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_PROPERTY_DOMAIN;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLObjectProperty property = ctx.pools().objectProperties().pick(ctx.random());
			OWLClass domain = ctx.pools().classes().pick(ctx.random());
			axioms.add(ctx.dataFactory().getOWLObjectPropertyDomainAxiom(property, domain));
		}
		return axioms;
	}
}
