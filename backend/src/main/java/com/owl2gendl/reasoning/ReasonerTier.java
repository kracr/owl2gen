package com.owl2gendl.reasoning;

/**
 * {@code OPENLLET} and {@code HERMIT} are in-JVM OWL API reasoners, created via
 * {@link VerificationService#createReasoner}. {@code KONCLUDE} is fundamentally different: it has no OWL API
 * binding, so it runs as an external process (see {@code KoncludeConsistencyChecker}) invoked directly rather
 * than through {@code createReasoner}. {@code CUSTOM} routes to a user-supplied {@code OWLReasonerFactory}
 * Spring bean (see {@code CustomReasonerFactory}) — the intended extension point for a reasoner this project
 * does not ship with.
 */
public enum ReasonerTier {
	OPENLLET, HERMIT, KONCLUDE, CUSTOM
}
