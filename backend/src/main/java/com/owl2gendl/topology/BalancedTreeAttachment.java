package com.owl2gendl.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Always reuses one of the least-used still-reusable entities, spreading load evenly across the pool —
 * produces shallow, wide, balanced shapes rather than deep chains or hub-dominated graphs.
 *
 * <p>Ties are broken randomly rather than always taking the first-found minimum. Deterministic first-found
 * tie-breaking sounds harmless but is not: combined with cycle-avoiding callers like
 * {@code DepthBiasedHierarchyStrategy}, it makes every call with the same tied usage counts resolve the same
 * way, which serializes an entire small pool into one fully-ordered processing sequence instead of a genuinely
 * spread-out one — observed in practice as small pools collapsing into a single linear chain (the opposite of
 * "balanced") once cycle-avoidance exhausted the deterministic ordering's reuse options.
 */
public class BalancedTreeAttachment implements AttachmentStrategy {

	@Override
	public <T> T choose(List<T> reusableCandidates, ToIntFunction<T> usageCount, Random random) {
		int bestUsage = Integer.MAX_VALUE;
		List<T> tiedForBest = new ArrayList<>();
		for (T candidate : reusableCandidates) {
			int usage = usageCount.applyAsInt(candidate);
			if (usage < bestUsage) {
				bestUsage = usage;
				tiedForBest.clear();
				tiedForBest.add(candidate);
			} else if (usage == bestUsage) {
				tiedForBest.add(candidate);
			}
		}
		return tiedForBest.get(random.nextInt(tiedForBest.size()));
	}
}
