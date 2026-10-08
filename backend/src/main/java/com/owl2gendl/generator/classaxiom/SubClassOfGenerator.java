package com.owl2gendl.generator.classaxiom;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;
import com.owl2gendl.pool.ClassPool;

@Component
public class SubClassOfGenerator implements ConstructGenerator {

	@Override
	public ConstructId id() {
		return ConstructId.SUB_CLASS_OF;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		ClassPool classes = ctx.pools().classes();
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass sub = classes.pick(ctx.random());
			OWLClass sup = ctx.hierarchyStrategy().chooseSuperclass(ctx, sub);
			axioms.add(ctx.dataFactory().getOWLSubClassOfAxiom(sub, sup));
		}
		return axioms;
	}
}
