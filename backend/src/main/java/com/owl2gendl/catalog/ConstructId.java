package com.owl2gendl.catalog;

import static com.owl2gendl.catalog.ConstructCategory.ASSERTION;
import static com.owl2gendl.catalog.ConstructCategory.CLASS_AXIOM;
import static com.owl2gendl.catalog.ConstructCategory.CLASS_EXPRESSION;
import static com.owl2gendl.catalog.ConstructCategory.DATA_PROPERTY_AXIOM;
import static com.owl2gendl.catalog.ConstructCategory.DATA_PROPERTY_RESTRICTION;
import static com.owl2gendl.catalog.ConstructCategory.DATA_RANGE;
import static com.owl2gendl.catalog.ConstructCategory.OBJECT_PROPERTY_AXIOM;
import static com.owl2gendl.catalog.ConstructCategory.OBJECT_PROPERTY_RESTRICTION;

/**
 * The single source of truth for every OWL2 construct OWL2Gen-DL can generate. Every value here must have
 * exactly one registered {@code com.owl2gendl.generator.ConstructGenerator} — enforced at startup by
 * {@code ConstructGeneratorRegistry} — so a construct can never be silently present in code but absent from
 * the UI/API, or vice versa.
 *
 * <p>{@code supportsNesting} marks constructs that can serve as a recursively-nestable filler inside another
 * class/data-range expression (consulted by the topology layer's nesting strategy). {@code requiresIndividuals}
 * marks constructs whose generation needs named individuals to already exist in the entity pool.
 *
 * <p>{@code elEligible} marks constructs whose axiom shape is legal under the OWL 2 EL profile. This is a
 * hand-set best-effort classification, not the source of truth — {@code ConstructProfileEligibilityTest}
 * generates one real sample axiom per construct via its actual registered generator and checks it against
 * OWL API's authoritative {@code OWL2ELProfile} checker, failing loudly if this flag ever disagrees with it.
 */
public enum ConstructId {

	// --- Class expressions & enumerations ---
	OBJECT_COMPLEMENT_OF(CLASS_EXPRESSION, "Object Complement Of", "The complement of a class expression.", true, false, false),
	OBJECT_INTERSECTION_OF(CLASS_EXPRESSION, "Object Intersection Of", "The intersection of two or more class expressions.", true, false, true),
	// elEligible=false: EL's nominal-class grammar permits exactly one individual, but this generator
	// always uses two (see ObjectOneOfGenerator) — a real mismatch, not a hand-typed-table mistake.
	OBJECT_ONE_OF(CLASS_EXPRESSION, "Object One Of", "An enumerated class of named individuals.", false, true, false),
	OBJECT_UNION_OF(CLASS_EXPRESSION, "Object Union Of", "The union of two or more class expressions.", true, false, false),

	// --- Class axioms ---
	DISJOINT_UNION(CLASS_AXIOM, "Disjoint Union", "A class is the disjoint union of a set of subclasses.", false, false, false),
	DISJOINT_WITH(CLASS_AXIOM, "Disjoint With", "Two classes share no instances.", false, false, true),
	EQUIVALENT_CLASSES(CLASS_AXIOM, "Equivalent Classes", "Two class expressions denote the same class.", false, false, true),
	SUB_CLASS_OF(CLASS_AXIOM, "Sub Class Of", "One class is a subclass of another.", false, false, true),
	ALL_DISJOINT_CLASSES(CLASS_AXIOM, "All Disjoint Classes", "Pairwise disjointness over three or more classes.", false, false, true),

	// --- Object property axioms ---
	ASYMMETRIC_PROPERTY(OBJECT_PROPERTY_AXIOM, "Asymmetric Property", "An object property that is asymmetric.", false, false, false),
	EQUIVALENT_OBJECT_PROPERTY(OBJECT_PROPERTY_AXIOM, "Equivalent Object Property", "Two object properties denote the same relation.", false, false, true),
	FUNCTIONAL_OBJECT_PROPERTY(OBJECT_PROPERTY_AXIOM, "Functional Object Property", "An object property with at most one value per subject.", false, false, false),
	INVERSE_FUNCTIONAL_PROPERTY(OBJECT_PROPERTY_AXIOM, "Inverse Functional Property", "An object property with at most one subject per value.", false, false, false),
	INVERSE_OF_PROPERTY(OBJECT_PROPERTY_AXIOM, "Inverse Of Property", "One object property is the inverse of another.", false, false, false),
	IRREFLEXIVE_PROPERTY(OBJECT_PROPERTY_AXIOM, "Irreflexive Property", "An object property that never relates an individual to itself.", false, false, false),
	OBJECT_PROPERTY_DISJOINT_WITH(OBJECT_PROPERTY_AXIOM, "Object Property Disjoint With", "Two object properties share no instance pairs.", false, false, false),
	REFLEXIVE_PROPERTY(OBJECT_PROPERTY_AXIOM, "Reflexive Property", "An object property that always relates an individual to itself.", false, false, true),
	SYMMETRIC_PROPERTY(OBJECT_PROPERTY_AXIOM, "Symmetric Property", "An object property that is symmetric.", false, false, false),
	TRANSITIVE_PROPERTY(OBJECT_PROPERTY_AXIOM, "Transitive Property", "An object property that is transitive.", false, false, true),
	OBJECT_PROPERTY_DOMAIN(OBJECT_PROPERTY_AXIOM, "Object Property Domain", "Restricts the domain of an object property.", false, false, true),
	OBJECT_PROPERTY_RANGE(OBJECT_PROPERTY_AXIOM, "Object Property Range", "Restricts the range of an object property.", false, false, true),
	PROPERTY_CHAIN_AXIOM(OBJECT_PROPERTY_AXIOM, "Property Chain Axiom", "A chain of object properties implies another object property.", false, false, true),
	SUB_OBJECT_PROPERTY_OF(OBJECT_PROPERTY_AXIOM, "Sub Object Property Of", "One object property is a sub-property of another.", false, false, true),
	ALL_DISJOINT_OBJECT_PROPERTIES(OBJECT_PROPERTY_AXIOM, "All Disjoint Object Properties", "Pairwise disjointness over three or more object properties.", false, false, false),

	// --- Object property restrictions ---
	OBJECT_ALL_VALUES_FROM(OBJECT_PROPERTY_RESTRICTION, "Object All Values From", "Universal restriction along an object property.", true, false, false),
	OBJECT_HAS_SELF(OBJECT_PROPERTY_RESTRICTION, "Object Has Self", "Restriction requiring self-relation via an object property.", true, false, true),
	OBJECT_HAS_VALUE(OBJECT_PROPERTY_RESTRICTION, "Object Has Value", "Restriction requiring a specific individual as filler.", true, true, true),
	OBJECT_SOME_VALUES_FROM(OBJECT_PROPERTY_RESTRICTION, "Object Some Values From", "Existential restriction along an object property.", true, false, true),
	OBJECT_MIN_CARDINALITY(OBJECT_PROPERTY_RESTRICTION, "Object Min Cardinality", "Unqualified minimum cardinality restriction.", true, false, false),
	OBJECT_MAX_CARDINALITY(OBJECT_PROPERTY_RESTRICTION, "Object Max Cardinality", "Unqualified maximum cardinality restriction.", true, false, false),
	OBJECT_EXACT_CARDINALITY(OBJECT_PROPERTY_RESTRICTION, "Object Exact Cardinality", "Unqualified exact cardinality restriction.", true, false, false),
	OBJECT_MIN_QUALIFIED_CARDINALITY(OBJECT_PROPERTY_RESTRICTION, "Object Min Qualified Cardinality", "Qualified minimum cardinality restriction.", true, false, false),
	OBJECT_MAX_QUALIFIED_CARDINALITY(OBJECT_PROPERTY_RESTRICTION, "Object Max Qualified Cardinality", "Qualified maximum cardinality restriction.", true, false, false),
	OBJECT_QUALIFIED_CARDINALITY(OBJECT_PROPERTY_RESTRICTION, "Object Qualified Cardinality", "Qualified exact cardinality restriction.", true, false, false),

	// --- Data property axioms ---
	EQUIVALENT_DATA_PROPERTY(DATA_PROPERTY_AXIOM, "Equivalent Data Property", "Two data properties denote the same relation.", false, false, true),
	FUNCTIONAL_DATA_PROPERTY(DATA_PROPERTY_AXIOM, "Functional Data Property", "A data property with at most one value per subject.", false, false, true),
	DATA_PROPERTY_DISJOINT_WITH(DATA_PROPERTY_AXIOM, "Data Property Disjoint With", "Two data properties share no instance pairs.", false, false, false),
	DATA_PROPERTY_DOMAIN(DATA_PROPERTY_AXIOM, "Data Property Domain", "Restricts the domain of a data property.", false, false, true),
	DATA_PROPERTY_RANGE(DATA_PROPERTY_AXIOM, "Data Property Range", "Restricts the range of a data property.", false, false, true),
	SUB_DATA_PROPERTY_OF(DATA_PROPERTY_AXIOM, "Sub Data Property Of", "One data property is a sub-property of another.", false, false, true),
	ALL_DISJOINT_DATA_PROPERTIES(DATA_PROPERTY_AXIOM, "All Disjoint Data Properties", "Pairwise disjointness over three or more data properties.", false, false, false),

	// --- Data property restrictions ---
	DATA_ALL_VALUES_FROM(DATA_PROPERTY_RESTRICTION, "Data All Values From", "Universal restriction along a data property.", true, false, false),
	DATA_HAS_VALUE(DATA_PROPERTY_RESTRICTION, "Data Has Value", "Restriction requiring a specific literal as filler.", true, false, true),
	DATA_SOME_VALUES_FROM(DATA_PROPERTY_RESTRICTION, "Data Some Values From", "Existential restriction along a data property.", true, false, true),
	DATA_MIN_CARDINALITY(DATA_PROPERTY_RESTRICTION, "Data Min Cardinality", "Unqualified minimum cardinality restriction.", true, false, false),
	DATA_MAX_CARDINALITY(DATA_PROPERTY_RESTRICTION, "Data Max Cardinality", "Unqualified maximum cardinality restriction.", true, false, false),
	DATA_EXACT_CARDINALITY(DATA_PROPERTY_RESTRICTION, "Data Exact Cardinality", "Unqualified exact cardinality restriction.", true, false, false),
	DATA_MIN_QUALIFIED_CARDINALITY(DATA_PROPERTY_RESTRICTION, "Data Min Qualified Cardinality", "Qualified minimum cardinality restriction.", true, false, false),
	DATA_MAX_QUALIFIED_CARDINALITY(DATA_PROPERTY_RESTRICTION, "Data Max Qualified Cardinality", "Qualified maximum cardinality restriction.", true, false, false),
	DATA_QUALIFIED_CARDINALITY(DATA_PROPERTY_RESTRICTION, "Data Qualified Cardinality", "Qualified exact cardinality restriction.", true, false, false),

	// --- Data ranges ---
	DATA_COMPLEMENT_OF(DATA_RANGE, "Data Complement Of", "The complement of a data range.", true, false, false),
	DATA_INTERSECTION_OF(DATA_RANGE, "Data Intersection Of", "The intersection of two or more data ranges.", true, false, false),
	DATA_ONE_OF(DATA_RANGE, "Data One Of", "An enumerated data range of literals.", false, false, false),
	DATA_UNION_OF(DATA_RANGE, "Data Union Of", "The union of two or more data ranges.", true, false, false),
	DATATYPE_DEFINITION(DATA_RANGE, "Datatype Definition", "Defines a new datatype equivalent to a data range.", false, false, false),
	DATATYPE_RESTRICTION(DATA_RANGE, "Datatype Restriction", "A datatype restricted by facets (e.g. xsd:minInclusive).", false, false, false),

	// --- Assertions & keys ---
	HAS_KEY(ASSERTION, "Has Key", "A set of properties that uniquely identify instances of a class.", false, false, true),
	CLASS_ASSERTION(ASSERTION, "Class Assertion", "Asserts that an individual is an instance of a class.", false, true, true),
	OBJECT_PROPERTY_ASSERTION(ASSERTION, "Object Property Assertion", "Asserts an object property relation between two individuals.", false, true, true),
	DATA_PROPERTY_ASSERTION(ASSERTION, "Data Property Assertion", "Asserts a data property relation between an individual and a literal.", false, true, true),
	SAME_INDIVIDUAL(ASSERTION, "Same Individual", "Asserts that two individuals are the same.", false, true, true),
	DIFFERENT_INDIVIDUALS(ASSERTION, "Different Individuals", "Asserts that two individuals are different.", false, true, true),
	NEGATIVE_OBJECT_PROPERTY_ASSERTION(ASSERTION, "Negative Object Property Assertion", "Asserts that an object property relation does not hold.", false, true, true),
	NEGATIVE_DATA_PROPERTY_ASSERTION(ASSERTION, "Negative Data Property Assertion", "Asserts that a data property relation does not hold.", false, true, true);

	private final ConstructCategory category;
	private final String displayName;
	private final String description;
	private final boolean supportsNesting;
	private final boolean requiresIndividuals;
	private final boolean elEligible;

	ConstructId(ConstructCategory category, String displayName, String description, boolean supportsNesting,
			boolean requiresIndividuals, boolean elEligible) {
		this.category = category;
		this.displayName = displayName;
		this.description = description;
		this.supportsNesting = supportsNesting;
		this.requiresIndividuals = requiresIndividuals;
		this.elEligible = elEligible;
	}

	public ConstructCategory category() {
		return category;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}

	public boolean supportsNesting() {
		return supportsNesting;
	}

	public boolean requiresIndividuals() {
		return requiresIndividuals;
	}

	public boolean elEligible() {
		return elEligible;
	}
}
