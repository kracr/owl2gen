package com.owl2gendl.constraint;

import java.util.List;

import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;

/**
 * The registry of known clash pairs — formalizes the old codebase's ad-hoc
 * {@code discardConcepts}/{@code discardProperties}/{@code notToBeIncludedAxioms}/{@code alreadyAssertionAxiom}
 * bookkeeping (scattered across {@code app.java} and several category classes) into one declarative list.
 * Stateless and shared: a fresh {@link ConstraintTracker} is created per generation run and consults this
 * list, so concurrent runs never share the per-run "what's already applied" state.
 */
@Component
public class ConstraintRuleRegistry {

	private final List<ClashRule> rules = List.of(
			new ClashRule(ConstructId.ASYMMETRIC_PROPERTY, ConstructId.SYMMETRIC_PROPERTY),
			new ClashRule(ConstructId.REFLEXIVE_PROPERTY, ConstructId.IRREFLEXIVE_PROPERTY),
			// Asymmetric implies irreflexive (P(x,x) would make it both hold and not hold for x,x), which
			// directly contradicts Reflexive forcing P(x,x) for every individual — unconditionally unsatisfiable.
			new ClashRule(ConstructId.REFLEXIVE_PROPERTY, ConstructId.ASYMMETRIC_PROPERTY),
			new ClashRule(ConstructId.DISJOINT_WITH, ConstructId.EQUIVALENT_CLASSES),
			new ClashRule(ConstructId.OBJECT_PROPERTY_DISJOINT_WITH, ConstructId.EQUIVALENT_OBJECT_PROPERTY),
			new ClashRule(ConstructId.DATA_PROPERTY_DISJOINT_WITH, ConstructId.EQUIVALENT_DATA_PROPERTY),
			// A class requiring hasSelf via a property also declared Irreflexive is unsatisfiable — the
			// same clash shape as Reflexive/Irreflexive above, just not on the property axiom itself.
			new ClashRule(ConstructId.OBJECT_HAS_SELF, ConstructId.IRREFLEXIVE_PROPERTY),
			// Asserting and negating the same (property, subject, object) triple is a direct contradiction.
			new ClashRule(ConstructId.OBJECT_PROPERTY_ASSERTION, ConstructId.NEGATIVE_OBJECT_PROPERTY_ASSERTION));

	public List<ClashRule> rules() {
		return rules;
	}
}
