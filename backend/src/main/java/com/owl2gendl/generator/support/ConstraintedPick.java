package com.owl2gendl.generator.support;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Retries a pool pick a bounded number of times looking for one the {@link com.owl2gendl.constraint.ConstraintTracker}
 * will allow, rather than forcing a known clash. Returns {@code null} if nothing eligible turns up within the
 * attempt budget, in which case the caller should skip that axiom instance rather than generate a clash.
 */
public final class ConstraintedPick {

	private static final int MAX_ATTEMPTS = 8;

	private ConstraintedPick() {
	}

	public static <T> T find(Supplier<T> candidateSupplier, Predicate<T> isAllowed) {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			T candidate = candidateSupplier.get();
			if (isAllowed.test(candidate)) {
				return candidate;
			}
		}
		return null;
	}
}
