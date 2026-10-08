package com.owl2gendl.generator.datarange;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/** Names a new datatype equivalent to the built-in {@code xsd:integer}. */
@Component
public class DatatypeDefinitionGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATATYPE_DEFINITION;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDatatype subject = ctx.pools().datatypes().mintNew();
			axioms.add(ctx.dataFactory().getOWLDatatypeDefinitionAxiom(subject, ctx.dataFactory().getIntegerOWLDatatype()));
		}
		return axioms;
	}
}
