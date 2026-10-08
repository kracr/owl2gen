package com.owl2gendl.topology;

import org.semanticweb.owlapi.model.OWLClassExpression;

import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.reasoning.TargetProfile;

/**
 * With probability {@code nestingProbability} (and while under {@code maxDepth}), recurses into a compound
 * class expression instead of terminating at a plain pooled class. A probability less than 1.0 matters as
 * much as the depth cap: always nesting to the cap would make every axiom uniformly maximally complex, which
 * is exactly as unrealistic as never nesting.
 *
 * <p>Under {@link TargetProfile#EL}, the choice of compound expression is restricted to
 * {@code ObjectIntersectionOf} only — {@code ObjectComplementOf} and {@code ObjectUnionOf} are not legal in
 * OWL 2 EL, and without this restriction nesting could silently break the profile guarantee a request made,
 * regardless of how carefully the top-level construct selection was filtered.
 */
public class ProbabilisticNestingStrategy implements NestingStrategy {

	private static final int DISTINCT_FILLER_ATTEMPTS = 8;

	private final int maxDepth;
	private final double nestingProbability;
	private final TargetProfile targetProfile;

	/**
	 * {@code configuredMaxDepth} is the depth an operator sets and the one {@link NestingDepthAnalyzer}
	 * reports back — i.e. the depth of the whole class expression, including the restriction or boolean
	 * construct that every {@code buildClassFiller} call site wraps its result in
	 * (see {@code ObjectSomeValuesFromGenerator} et al., which all invoke this at {@code currentDepth=0}
	 * before embedding the result in their own construct). That wrapping construct always contributes one
	 * level of measured depth that this class's own recursion never sees or controls, so internally this
	 * caps recursion one level short of the configured value - otherwise the measured maximum would always
	 * land one level past what was configured (confirmed empirically: {@code m=2} produced a measured max
	 * of 3, not 2).
	 */
	public ProbabilisticNestingStrategy(int configuredMaxDepth, double nestingProbability, TargetProfile targetProfile) {
		this.maxDepth = Math.max(0, configuredMaxDepth - 1);
		this.nestingProbability = Math.min(1.0, Math.max(0.0, nestingProbability));
		this.targetProfile = targetProfile;
	}

	@Override
	public OWLClassExpression buildClassFiller(GenerationContext ctx, int currentDepth) {
		if (currentDepth >= maxDepth || ctx.random().nextDouble() >= nestingProbability) {
			return ctx.pools().classes().pick(ctx.random());
		}
		if (targetProfile == TargetProfile.EL) {
			OWLClassExpression a = buildClassFiller(ctx, currentDepth + 1);
			OWLClassExpression b = buildDistinctClassFiller(ctx, currentDepth + 1, a);
			return ctx.dataFactory().getOWLObjectIntersectionOf(a, b);
		}
		int choice = ctx.random().nextInt(3);
		return switch (choice) {
			case 0 -> ctx.dataFactory().getOWLObjectComplementOf(buildClassFiller(ctx, currentDepth + 1));
			case 1 -> {
				OWLClassExpression a = buildClassFiller(ctx, currentDepth + 1);
				OWLClassExpression b = buildDistinctClassFiller(ctx, currentDepth + 1, a);
				yield ctx.dataFactory().getOWLObjectIntersectionOf(a, b);
			}
			default -> {
				OWLClassExpression a = buildClassFiller(ctx, currentDepth + 1);
				OWLClassExpression b = buildDistinctClassFiller(ctx, currentDepth + 1, a);
				yield ctx.dataFactory().getOWLObjectUnionOf(a, b);
			}
		};
	}

	/**
	 * {@code ObjectIntersectionOf}/{@code ObjectUnionOf} are backed by a {@code Set} in OWL API, so if two
	 * independently-built fillers happen to be equal, the expression silently collapses to a single operand
	 * — invalid OWL2 in every profile. Retries a bounded number of times, then falls through to a freshly
	 * minted class, which is guaranteed distinct from anything already built — the same bounded-retry-then-
	 * guaranteed-fallback pattern {@code EntityPool.pickDifferentFrom} already uses for the same reason.
	 */
	@Override
	public OWLClassExpression buildDistinctClassFiller(GenerationContext ctx, int currentDepth, OWLClassExpression excluding) {
		OWLClassExpression candidate = buildClassFiller(ctx, currentDepth);
		for (int attempt = 0; candidate.equals(excluding) && attempt < DISTINCT_FILLER_ATTEMPTS; attempt++) {
			candidate = buildClassFiller(ctx, currentDepth);
		}
		return candidate.equals(excluding) ? ctx.pools().classes().mintNew() : candidate;
	}
}
