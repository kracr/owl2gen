package com.owl2gendl.generator.datarange;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.Literals;

/** Names a datatype equivalent to an enumeration of literals. */
@Component
public class DataOneOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATA_ONE_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDatatype subject = ctx.pools().datatypes().mintNew();
			OWLDataRange oneOf = ctx.dataFactory().getOWLDataOneOf(
					Literals.randomString(ctx.dataFactory(), ctx.random()),
					Literals.randomString(ctx.dataFactory(), ctx.random()));
			axioms.add(ctx.dataFactory().getOWLDatatypeDefinitionAxiom(subject, oneOf));
		}
		return axioms;
	}
}
