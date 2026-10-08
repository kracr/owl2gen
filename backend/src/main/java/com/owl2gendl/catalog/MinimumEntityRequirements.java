package com.owl2gendl.catalog;

import java.util.Map;

/**
 * How many distinct entities of each type a construct needs at minimum to produce even one structurally
 * valid axiom - used to size default entity pools when the user leaves counts unset, and to reject explicit,
 * too-low user-provided counts before generation starts rather than let the pool silently mint its way
 * around them.
 *
 * <p>Deliberately coarse, not an exhaustive hand-verified table like {@link ConstructId#elEligible()} - most
 * constructs just need 1 of their primary type (the general case, handled by the per-category fallback in
 * {@link #forConstruct}); a short list of exceptions need 2 (pairwise relations: SubClassOf,
 * EquivalentClasses, DisjointWith, InverseOf, ...) or 3 (n-ary disjointness / DisjointUnion). Getting one of
 * these a little conservative just means an extra minted entity or an occasional unnecessary confirmation
 * prompt, not a correctness bug - the entity pools already auto-mint on demand regardless (see
 * {@code EntityPool}).
 */
public final class MinimumEntityRequirements {

	private static final Map<ConstructId, EntityRequirement> OVERRIDES = Map.ofEntries(
			// --- needs 2 distinct classes ---
			Map.entry(ConstructId.OBJECT_INTERSECTION_OF, new EntityRequirement(2, 0, 0, 0)),
			Map.entry(ConstructId.OBJECT_UNION_OF, new EntityRequirement(2, 0, 0, 0)),
			Map.entry(ConstructId.DISJOINT_WITH, new EntityRequirement(2, 0, 0, 0)),
			Map.entry(ConstructId.EQUIVALENT_CLASSES, new EntityRequirement(2, 0, 0, 0)),
			Map.entry(ConstructId.SUB_CLASS_OF, new EntityRequirement(2, 0, 0, 0)),
			// --- needs 3 distinct classes ---
			Map.entry(ConstructId.DISJOINT_UNION, new EntityRequirement(3, 0, 0, 0)),
			Map.entry(ConstructId.ALL_DISJOINT_CLASSES, new EntityRequirement(3, 0, 0, 0)),
			// --- needs 2 distinct object properties ---
			Map.entry(ConstructId.EQUIVALENT_OBJECT_PROPERTY, new EntityRequirement(0, 2, 0, 0)),
			Map.entry(ConstructId.INVERSE_OF_PROPERTY, new EntityRequirement(0, 2, 0, 0)),
			Map.entry(ConstructId.OBJECT_PROPERTY_DISJOINT_WITH, new EntityRequirement(0, 2, 0, 0)),
			Map.entry(ConstructId.PROPERTY_CHAIN_AXIOM, new EntityRequirement(0, 2, 0, 0)),
			Map.entry(ConstructId.SUB_OBJECT_PROPERTY_OF, new EntityRequirement(0, 2, 0, 0)),
			// --- needs 3 distinct object properties ---
			Map.entry(ConstructId.ALL_DISJOINT_OBJECT_PROPERTIES, new EntityRequirement(0, 3, 0, 0)),
			// --- needs 2 distinct data properties ---
			Map.entry(ConstructId.EQUIVALENT_DATA_PROPERTY, new EntityRequirement(0, 0, 2, 0)),
			Map.entry(ConstructId.DATA_PROPERTY_DISJOINT_WITH, new EntityRequirement(0, 0, 2, 0)),
			Map.entry(ConstructId.SUB_DATA_PROPERTY_OF, new EntityRequirement(0, 0, 2, 0)),
			// --- needs 3 distinct data properties ---
			Map.entry(ConstructId.ALL_DISJOINT_DATA_PROPERTIES, new EntityRequirement(0, 0, 3, 0)),
			// --- data ranges are datatype-only, no entity pool involvement ---
			Map.entry(ConstructId.DATA_COMPLEMENT_OF, EntityRequirement.none()),
			Map.entry(ConstructId.DATA_INTERSECTION_OF, EntityRequirement.none()),
			Map.entry(ConstructId.DATA_ONE_OF, EntityRequirement.none()),
			Map.entry(ConstructId.DATA_UNION_OF, EntityRequirement.none()),
			Map.entry(ConstructId.DATATYPE_DEFINITION, EntityRequirement.none()),
			Map.entry(ConstructId.DATATYPE_RESTRICTION, EntityRequirement.none()),
			// --- needs 2 distinct individuals (beyond the generic requiresIndividuals()-implied 1) ---
			Map.entry(ConstructId.SAME_INDIVIDUAL, new EntityRequirement(0, 0, 0, 2)),
			Map.entry(ConstructId.DIFFERENT_INDIVIDUALS, new EntityRequirement(0, 0, 0, 2)),
			Map.entry(ConstructId.OBJECT_PROPERTY_ASSERTION, new EntityRequirement(0, 1, 0, 2)),
			Map.entry(ConstructId.NEGATIVE_OBJECT_PROPERTY_ASSERTION, new EntityRequirement(0, 1, 0, 2)),
			// --- assertions needing a data property rather than the ASSERTION default's implied class ---
			Map.entry(ConstructId.DATA_PROPERTY_ASSERTION, new EntityRequirement(0, 0, 1, 1)),
			Map.entry(ConstructId.NEGATIVE_DATA_PROPERTY_ASSERTION, new EntityRequirement(0, 0, 1, 1)),
			Map.entry(ConstructId.HAS_KEY, new EntityRequirement(1, 0, 0, 1)));

	private MinimumEntityRequirements() {
	}

	public static EntityRequirement forConstruct(ConstructId id) {
		EntityRequirement override = OVERRIDES.get(id);
		if (override != null) {
			return override;
		}
		int individuals = id.requiresIndividuals() ? 1 : 0;
		return switch (id.category()) {
			case CLASS_EXPRESSION, CLASS_AXIOM -> new EntityRequirement(1, 0, 0, individuals);
			case OBJECT_PROPERTY_AXIOM -> new EntityRequirement(0, 1, 0, individuals);
			case OBJECT_PROPERTY_RESTRICTION -> new EntityRequirement(1, 1, 0, individuals);
			case DATA_PROPERTY_AXIOM -> new EntityRequirement(0, 0, 1, individuals);
			case DATA_PROPERTY_RESTRICTION -> new EntityRequirement(1, 0, 1, individuals);
			case DATA_RANGE -> EntityRequirement.none();
			case ASSERTION -> new EntityRequirement(1, 0, 0, Math.max(individuals, 1));
		};
	}

	public static EntityRequirement forSelection(Iterable<ConstructId> selected) {
		EntityRequirement total = EntityRequirement.none();
		for (ConstructId id : selected) {
			total = total.max(forConstruct(id));
		}
		return total;
	}
}
