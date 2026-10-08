package com.owl2gendl.reasoning;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.springframework.stereotype.Component;

/**
 * Wraps OWL API's authoritative {@code OWLProfile} checkers. This is what {@link ReasonerTierSelector} uses
 * to decide whether ELK (fast, polynomial, EL-only) is applicable, rather than the catalog's per-construct
 * "profile hint" metadata, which is informational only.
 */
@Component
public class OwlProfileChecker {

	public ProfileCheckResult check(OWLOntology ontology) {
		boolean el = new OWL2ELProfile().checkOntology(ontology).isInProfile();
		boolean ql = new OWL2QLProfile().checkOntology(ontology).isInProfile();
		boolean rl = new OWL2RLProfile().checkOntology(ontology).isInProfile();
		boolean dl = new OWL2DLProfile().checkOntology(ontology).isInProfile();
		return new ProfileCheckResult(el, ql, rl, dl);
	}
}
