package com.owl2gendl.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * {@code tier} is optional and selects the reasoner used for this variant's final verification
 * ({@code VariantOrchestrator}) - one of {@code OPENLLET, HERMIT, KONCLUDE, CUSTOM} (see
 * {@link com.owl2gendl.reasoning.ReasonerTier}); {@code null} keeps the default (Openllet). Independent of
 * {@code owl2gendl.reasoning.fallback-order}, which only governs intermediate per-batch checks under
 * {@code strictConsistency} - this field is the only way to run final verification under a reasoner other
 * than Openllet.
 */
public record ReasoningConfigDto(@Min(1) @Max(300) Integer timeoutSeconds, String tier) {
}
