package com.owl2gendl.topology;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Biases selection toward already-well-used entities (weight proportional to usageCount + 1) — hub
 * entities get more connections, producing a scale-free-like degree distribution similar to real-world
 * ontologies, rather than the flat distribution uniform-random attachment produces.
 */
public class PreferentialAttachment implements AttachmentStrategy {

	@Override
	public <T> T choose(List<T> reusableCandidates, ToIntFunction<T> usageCount, Random random) {
		int totalWeight = 0;
		int[] weights = new int[reusableCandidates.size()];
		for (int i = 0; i < reusableCandidates.size(); i++) {
			weights[i] = usageCount.applyAsInt(reusableCandidates.get(i)) + 1;
			totalWeight += weights[i];
		}
		int target = random.nextInt(totalWeight);
		int cumulative = 0;
		for (int i = 0; i < reusableCandidates.size(); i++) {
			cumulative += weights[i];
			if (target < cumulative) {
				return reusableCandidates.get(i);
			}
		}
		return reusableCandidates.get(reusableCandidates.size() - 1);
	}
}
