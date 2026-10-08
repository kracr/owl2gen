package com.owl2gendl.topology;

import org.semanticweb.owlapi.model.OWLClassExpression;

import com.owl2gendl.context.GenerationContext;

/**
 * Builds a filler class expression for expression-building constructs (restrictions, boolean combinators).
 * Nesting is deliberately separate from requested construct counts — a nested ObjectIntersectionOf built
 * here as a filler does not consume the user's explicit "ObjectIntersectionOf" count; it's structural
 * embellishment governed only by depth/probability, so requested counts always mean exactly that many
 * top-level axioms.
 */
public interface NestingStrategy {

	OWLClassExpression buildClassFiller(GenerationContext ctx, int currentDepth);

	/**
	 * Builds a filler guaranteed not to equal {@code excluding}. Needed anywhere two fillers are combined
	 * into a set-based expression (ObjectIntersectionOf/ObjectUnionOf are backed by a {@code Set} in OWL
	 * API): if two independently-built fillers happen to coincide, the resulting expression silently
	 * collapses to a single operand, which is invalid OWL2 in every profile, not just EL.
	 */
	OWLClassExpression buildDistinctClassFiller(GenerationContext ctx, int currentDepth, OWLClassExpression excluding);
}
