package com.owl2gendl.topology;

/**
 * The full set of structural parameters governing HOW a generated ontology is wired, independent of WHAT
 * constructs/counts were requested: which entity-attachment pattern to use, how deep/branchy the class
 * hierarchy should be, and how deep/likely nested class expressions should be.
 */
public record TopologyConfig(
		TopologyVariant variant,
		int hierarchyTargetDepth,
		int hierarchyBranchingFactor,
		int nestingMaxDepth,
		double nestingProbability) {

	public static TopologyConfig defaults() {
		return new TopologyConfig(TopologyVariant.UNIFORM_RANDOM, 4, 3, 2, 0.3);
	}
}
