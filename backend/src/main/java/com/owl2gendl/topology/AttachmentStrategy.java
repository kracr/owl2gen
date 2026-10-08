package com.owl2gendl.topology;

import java.util.List;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Decides WHICH pooled entity to reuse when a generator asks for one — decoupled from WHAT was requested
 * (that's the construct catalog/generators) and HOW MANY (that's the request's counts). This is what the old
 * project's "same axioms, different insertion order" 4-variant scheme should have been: genuinely different
 * wiring patterns from the same construct/count selection, not shuffled order.
 */
public interface AttachmentStrategy {

	<T> T choose(List<T> reusableCandidates, ToIntFunction<T> usageCount, Random random);
}
