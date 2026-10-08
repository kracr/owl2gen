package com.owl2gendl.metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.springframework.stereotype.Component;

/**
 * Walks every class expression appearing anywhere in the ontology's axioms and measures how deeply it
 * nests compound constructs (complement/intersection/union, restrictions) around a plain named class —
 * the direct measurement of the axiom-nestedness the topology layer's {@code NestingStrategy} controls.
 */
@Component
public class NestingDepthAnalyzer {

	public NestingMetrics analyze(OWLOntology ontology) {
		DepthVisitor visitor = new DepthVisitor();
		List<Integer> depths = new ArrayList<>();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			for (OWLClassExpression expression : axiom.getNestedClassExpressions()) {
				depths.add(expression.accept(visitor));
			}
		}
		if (depths.isEmpty()) {
			return new NestingMetrics(0, 0.0);
		}
		int max = depths.stream().mapToInt(Integer::intValue).max().orElse(0);
		double avg = depths.stream().mapToInt(Integer::intValue).average().orElse(0.0);
		return new NestingMetrics(max, avg);
	}

	/**
	 * Plain named classes and terminal restrictions (HasValue/HasSelf/OneOf, data restrictions) default to
	 * depth 0 — only the constructs that actually wrap another class expression add depth.
	 */
	private static final class DepthVisitor implements OWLClassExpressionVisitorEx<Integer> {

		@Override
		public Integer visit(OWLClass ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLObjectComplementOf ce) {
			return 1 + ce.getOperand().accept(this);
		}

		@Override
		public Integer visit(OWLObjectIntersectionOf ce) {
			return 1 + maxOperandDepth(ce.getOperands());
		}

		@Override
		public Integer visit(OWLObjectUnionOf ce) {
			return 1 + maxOperandDepth(ce.getOperands());
		}

		@Override
		public Integer visit(OWLObjectSomeValuesFrom ce) {
			return 1 + ce.getFiller().accept(this);
		}

		@Override
		public Integer visit(OWLObjectAllValuesFrom ce) {
			return 1 + ce.getFiller().accept(this);
		}

		@Override
		public Integer visit(OWLObjectMinCardinality ce) {
			return 1 + ce.getFiller().accept(this);
		}

		@Override
		public Integer visit(OWLObjectMaxCardinality ce) {
			return 1 + ce.getFiller().accept(this);
		}

		@Override
		public Integer visit(OWLObjectExactCardinality ce) {
			return 1 + ce.getFiller().accept(this);
		}

		@Override
		public Integer visit(OWLObjectHasValue ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLObjectHasSelf ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLObjectOneOf ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLDataSomeValuesFrom ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLDataAllValuesFrom ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLDataHasValue ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLDataMinCardinality ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLDataMaxCardinality ce) {
			return 0;
		}

		@Override
		public Integer visit(OWLDataExactCardinality ce) {
			return 0;
		}

		private int maxOperandDepth(Collection<OWLClassExpression> operands) {
			return operands.stream().mapToInt(o -> o.accept(this)).max().orElse(0);
		}
	}
}
