package com.owl2gendl.topology;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/** Erdős–Rényi-like: every reusable entity is equally likely to be picked. */
public class UniformRandomAttachment implements AttachmentStrategy {

	@Override
	public <T> T choose(List<T> reusableCandidates, ToIntFunction<T> usageCount, Random random) {
		return reusableCandidates.get(random.nextInt(reusableCandidates.size()));
	}
}
