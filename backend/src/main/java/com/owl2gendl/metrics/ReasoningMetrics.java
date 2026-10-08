package com.owl2gendl.metrics;

/**
 * {@code computed} is false when the reasoner could not classify the ontology (e.g. it's inconsistent, so
 * "everything entails everything" and the ratio is meaningless) rather than the metric genuinely being zero.
 */
public record ReasoningMetrics(boolean computed, int assertedSubClassOfCount, int inferredNewSubClassOfCount,
		double inferredToAssertedRatio) {

	public static ReasoningMetrics notComputed(int assertedSubClassOfCount) {
		return new ReasoningMetrics(false, assertedSubClassOfCount, 0, 0.0);
	}
}
