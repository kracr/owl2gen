package com.owl2gendl.generation;

/**
 * How {@link ConstructOrderPlanner} sequences the requested construct types before generation.
 *
 * <p>{@code DEFAULT} — {@link com.owl2gendl.catalog.ConstructId} declaration order: a no-op, exactly what
 * every request has always used. Kept as the silent default deliberately — see
 * {@link com.owl2gendl.orchestration.OntologyGenerationService}'s class-level doc for why a curated
 * reordering was tried under {@code strictConsistency} and reverted after it made things worse rather than
 * better.
 *
 * <p>{@code CATEGORY} — grouped by {@link com.owl2gendl.catalog.ConstructCategory}, in the order given by
 * {@code owl2gendl.generation.category-order}; within a category, constructs keep declaration order.
 *
 * <p>{@code RANDOM} — shuffled using the request's own seeded random source, so the order is reproducible
 * per seed but otherwise unconstrained; useful for exploring whether order affects a specific request.
 *
 * <p>{@code CUSTOM} — an explicit, user-supplied sequence (`owl2gendl.generation.custom-construct-order`).
 * Any requested construct type not mentioned in the custom list is appended afterward, in declaration order,
 * rather than silently dropped.
 *
 * <p>These modes are a control users can turn to explore their own construct mix; none is asserted here to
 * reduce reasoning cost or improve outcomes over another — that would need its own real experiment.
 */
public enum ConstructOrderMode {
	DEFAULT, CATEGORY, RANDOM, CUSTOM
}
