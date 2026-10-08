package com.owl2gendl.generator.datarange;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.Literals;

/** Names a datatype equivalent to {@code xsd:integer} restricted by a {@code minInclusive} facet. */
@Component
public class DatatypeRestrictionGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATATYPE_RESTRICTION;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLDatatype subject = ctx.pools().datatypes().mintNew();
			OWLFacetRestriction facetRestriction = ctx.dataFactory()
					.getOWLFacetRestriction(OWLFacet.MIN_INCLUSIVE, Literals.randomInteger(ctx.dataFactory(), ctx.random()));
			OWLDataRange restriction = ctx.dataFactory().getOWLDatatypeRestriction(ctx.dataFactory().getIntegerOWLDatatype(), facetRestriction);
			axioms.add(ctx.dataFactory().getOWLDatatypeDefinitionAxiom(subject, restriction));
		}
		return axioms;
	}
}
