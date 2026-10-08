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
import com.owl2gendl.generator.support.ConstraintedPick;

/** Exact qualified cardinality. */
@Component
public class DataQualifiedCardinalityGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.DATA_QUALIFIED_CARDINALITY;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			int cardinality = 1 + ctx.random().nextInt(3);
			OWLDataProperty property = cardinality < 2
					? ctx.pools().dataProperties().pick(ctx.random())
					: ConstraintedPick.find(
							() -> ctx.pools().dataProperties().pick(ctx.random()),
							p -> !ctx.constraintTracker().isFunctionalData(p));
			if (property == null) {
				continue; // every currently-eligible data property is already Functional
			}
			OWLClassExpression restriction = ctx.dataFactory().getOWLDataExactCardinality(cardinality, property, ctx.dataFactory().getStringOWLDatatype());
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(subject, restriction));
		}
		return axioms;
	}
}
