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

/** Names a datatype equivalent to {@code xsd:string ⊔ xsd:integer}. */
@Component
public class DataUnionOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATA_UNION_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDatatype subject = ctx.pools().datatypes().mintNew();
			OWLDataRange union = ctx.dataFactory().getOWLDataUnionOf(
					ctx.dataFactory().getStringOWLDatatype(), ctx.dataFactory().getIntegerOWLDatatype());
			axioms.add(ctx.dataFactory().getOWLDatatypeDefinitionAxiom(subject, union));
		}
		return axioms;
	}
}
