package com.owl2gendl.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** All fields optional — any omitted field falls back to {@code TopologyConfig.defaults()}. */
public record StructureConfigDto(
		@Min(1) @Max(20) Integer hierarchyTargetDepth,
		@Min(1) @Max(20) Integer hierarchyBranchingFactor,
		@Min(0) @Max(10) Integer nestingMaxDepth,
		@DecimalMin("0.0") @DecimalMax("1.0") Double nestingProbability) {
}
