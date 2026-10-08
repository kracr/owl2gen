package com.owl2gendl.metrics;

import java.time.Duration;

import org.semanticweb.owlapi.model.OWLOntology;
import org.springframework.stereotype.Service;

@Service
public class OntologyMetricsService {

	private final NestingDepthAnalyzer nestingDepthAnalyzer;
	private final HierarchyAnalyzer hierarchyAnalyzer;
	private final EntityRelationGraphBuilder graphBuilder;
	private final GraphMetricsCalculator graphMetricsCalculator;
	private final InferredVsAssertedCalculator inferredVsAssertedCalculator;

	public OntologyMetricsService(NestingDepthAnalyzer nestingDepthAnalyzer, HierarchyAnalyzer hierarchyAnalyzer,
			EntityRelationGraphBuilder graphBuilder, GraphMetricsCalculator graphMetricsCalculator,
			InferredVsAssertedCalculator inferredVsAssertedCalculator) {
		this.nestingDepthAnalyzer = nestingDepthAnalyzer;
		this.hierarchyAnalyzer = hierarchyAnalyzer;
		this.graphBuilder = graphBuilder;
		this.graphMetricsCalculator = graphMetricsCalculator;
		this.inferredVsAssertedCalculator = inferredVsAssertedCalculator;
	}

	/**
	 * @param reasoningTimeout bounds the inferred-vs-asserted step, which reuses the reasoner independently
	 *                         of {@code VerificationService} and can be more expensive than a plain
	 *                         consistency check (full classification vs. a single satisfiability query) - it
	 *                         needs its own explicit bound for the same reason verification does.
	 */
	public MetricsReport compute(OWLOntology ontology, Duration reasoningTimeout) {
		NestingMetrics nesting = nestingDepthAnalyzer.analyze(ontology);
		HierarchyMetrics hierarchy = hierarchyAnalyzer.analyze(ontology);
		GraphMetrics graph = graphMetricsCalculator.analyze(graphBuilder.build(ontology));
		ReasoningMetrics reasoning = inferredVsAssertedCalculator.analyze(ontology, reasoningTimeout);
		return new MetricsReport(nesting, hierarchy, graph, reasoning);
	}
}
