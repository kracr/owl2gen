package com.owl2gendl.generator.objectproprestriction;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.generator.support.ConstraintedPick;

@Component
public class ObjectHasSelfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.OBJECT_HAS_SELF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass subject = ctx.pools().classes().pick(ctx.random());
			OWLObjectProperty property = ConstraintedPick.find(
					() -> ctx.pools().objectProperties().pick(ctx.random()),
					p -> ctx.constraintTracker().canApply(id(), p) && ctx.constraintTracker().eligibleForSimpleRequired(p));
			if (property == null) {
				continue; // no property currently eligible without clashing with Irreflexive, or already non-simple
			}
			OWLClassExpression restriction = ctx.dataFactory().getOWLObjectHasSelf(property);
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(subject, restriction));
			ctx.constraintTracker().recordApplied(id(), property);
			ctx.constraintTracker().markSimpleRequired(property);
		}
		return axioms;
	}
}
