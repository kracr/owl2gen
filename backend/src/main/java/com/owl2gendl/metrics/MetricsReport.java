package com.owl2gendl.metrics;

/**
 * Structural/topological quality metrics for a generated ontology — deliberately not based on axiom-type
 * distribution or DL expressivity, since those are directly determined by what the user selected and would
 * just restate the input. These metrics instead capture HOW the selected axioms were wired together
 * (nesting, hierarchy shape, entity connectivity, inference potential), which is a function of the topology
 * strategy, not the user's construct/count choices.
 */
public record MetricsReport(
		NestingMetrics nesting,
		HierarchyMetrics hierarchy,
		GraphMetrics graph,
		ReasoningMetrics reasoning) {
}
