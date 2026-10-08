package com.owl2gendl.topology;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Always reuses the most-recently-minted still-reusable entity, producing deep, linear chains — a
 * worst-case shape for stress-testing transitive/chained reasoning.
 */
public class ChainAttachment implements AttachmentStrategy {

	@Override
	public <T> T choose(List<T> reusableCandidates, ToIntFunction<T> usageCount, Random random) {
		return reusableCandidates.get(reusableCandidates.size() - 1);
	}
}
