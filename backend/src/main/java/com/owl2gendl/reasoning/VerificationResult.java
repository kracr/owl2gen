package com.owl2gendl.reasoning;

/**
 * {@code requestedProfile} and {@code profileGuaranteeSatisfied} are the "trust, but verify" half of
 * profile-constrained generation: {@code profile} is what {@link OwlProfileChecker} actually found on the
 * generated ontology, independent of what was requested. {@code profileGuaranteeSatisfied} is {@code null}
 * when no profile was requested (target {@link TargetProfile#DL}, where the question doesn't apply), and
 * otherwise compares {@code requestedProfile} against {@code profile} — this should always be {@code true}
 * once construct/nesting filtering is correct, but the comparison must be made and reported, not assumed.
 */
public record VerificationResult(
		VerificationStatus status,
		ReasonerTier tier,
		ProfileCheckResult profile,
		long elapsedMillis,
		TargetProfile requestedProfile,
		Boolean profileGuaranteeSatisfied) {
}
