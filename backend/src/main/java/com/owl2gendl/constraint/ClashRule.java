package com.owl2gendl.constraint;

import com.owl2gendl.catalog.ConstructId;

/**
 * Declares that {@code first} and {@code second} must never both apply to the same entity (or same
 * unordered entity tuple, for pairwise facets like disjointness/equivalence between two specific classes).
 * A plain declarative registry entry — no if/else chain to extend when a new clash is identified.
 */
public record ClashRule(ConstructId first, ConstructId second) {
}
