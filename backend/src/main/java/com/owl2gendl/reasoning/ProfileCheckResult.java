package com.owl2gendl.reasoning;

/** Which OWL2 profiles an ontology's axioms comply with. */
public record ProfileCheckResult(boolean el, boolean ql, boolean rl, boolean dl) {
}
