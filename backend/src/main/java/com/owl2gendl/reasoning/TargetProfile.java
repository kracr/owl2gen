package com.owl2gendl.reasoning;

/**
 * A profile a user can request generation be restricted to. {@code DL} means today's unrestricted behavior
 * (all 67 catalog constructs available) — it is the default when no profile is specified. {@code EL} is
 * enforced end-to-end: construct selection (via {@link com.owl2gendl.catalog.ConstructId#elEligible()}),
 * generation-time nesting/datatype restrictions, and post-generation verification against OWL API's
 * authoritative {@code OWL2ELProfile} checker. QL and RL are deliberately not offered yet — both have
 * additional wrinkles (RL's asymmetric left/right-hand-side class-expression grammar, cross-profile datatype
 * restrictions) that deserve their own dedicated pass rather than a rushed addition alongside EL.
 */
public enum TargetProfile {
	DL, EL;

	/** {@code null} (or blank) resolves to {@code DL} — today's unrestricted behavior stays the default. */
	public static TargetProfile resolve(String raw) {
		if (raw == null || raw.isBlank()) {
			return DL;
		}
		return TargetProfile.valueOf(raw.toUpperCase());
	}
}
