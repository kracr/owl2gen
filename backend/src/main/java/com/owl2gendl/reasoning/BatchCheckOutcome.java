package com.owl2gendl.reasoning;

import java.util.Map;

/**
 * Result of {@link VerificationService#checkConsistencyWithFallback}.
 *
 * <p>{@code resolved} is {@code true} only when some tier in the fallback order actually reached a definite
 * CONSISTENT/INCONSISTENT answer within its budget. When {@code resolved} is {@code false}, every configured
 * tier timed out or errored, and {@code keepBatch} is {@code true} purely because this check fails open
 * (matching the original, single-reasoner behavior) — not because anyone confirmed the batch is fine. This
 * distinction, not just the final keep/drop decision, is what {@code BatchDiagnosticService} logs: an
 * unresolved batch that was silently waved through is exactly the case that used to leave no record at all.
 *
 * <p>{@code attempts} preserves the order tiers were tried in and every outcome observed — including cases
 * where an earlier tier failed but a later one succeeded, the evaluation-relevant signal this project wants
 * to collect.
 */
public record BatchCheckOutcome(boolean resolved, boolean keepBatch, Map<ReasonerTier, VerificationStatus> attempts) {
}
