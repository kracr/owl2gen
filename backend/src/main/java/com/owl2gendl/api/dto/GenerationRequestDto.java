package com.owl2gendl.api.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * {@code entityCounts}, {@code structure}, {@code variants}, and {@code reasoning} are all optional —
 * omitting {@code entityCounts} entirely (or any field within it) falls back to a resolved default via
 * {@code EntityCountsResolver}; omitting the others falls back to {@code TopologyConfig.defaults()}, a single
 * default-variant run, and a default timeout respectively. This keeps the M2 walking-skeleton sync endpoint
 * (which never reads these fields) working unchanged.
 *
 * <p>{@code targetProfile} is also optional; {@code null} (or {@code "DL"}) means today's unrestricted
 * behavior. A raw string, resolved to {@link com.owl2gendl.reasoning.TargetProfile} downstream, mirroring how
 * {@code variants} is a raw string list rather than a bound enum type.
 *
 * <p>{@code strictConsistency} is also optional; {@code null} (or {@code false}) means today's fast,
 * check-only-at-the-end behavior. {@code true} opts into per-construct-type reasoner checks during
 * generation (see {@code OntologyGenerationService}) - slower, but catches multi-hop logical inconsistencies
 * no local syntactic rule can.
 */
public record GenerationRequestDto(
		@Valid EntityCountsDto entityCounts,
		@NotNull Map<String, @Min(0) Integer> constructs,
		Long seed,
		@Valid StructureConfigDto structure,
		List<String> variants,
		@Valid ReasoningConfigDto reasoning,
		String targetProfile,
		Boolean strictConsistency) {
}
