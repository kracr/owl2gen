package com.owl2gendl.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.owl2gendl.topology.AttachmentStrategy;
import com.owl2gendl.topology.UniformRandomAttachment;

/**
 * A reuse-tracked pool of generated OWL entities (classes, properties, individuals). Fixes the old project's
 * bug where class reuse was hardcoded off (see ClassPool.MAX_REUSES=0 in the legacy codebase, which meant
 * every axiom minted a brand-new, disconnected class) — here reuse is on by default, and WHICH reusable
 * entity gets picked is delegated to a pluggable {@link AttachmentStrategy} (M4's topology layer) — every
 * generator that calls {@link #pick} automatically gets whichever attachment pattern the request asked for,
 * with no change to the generator's own code.
 *
 * <p>When every existing entity has hit {@code maxReuses}, {@link #pick} mints a new one automatically —
 * this is what guarantees a requested axiom count can always be satisfied even if the requested entity count
 * alone wouldn't support it (matching the "final axiom count may exceed the requested entity count because
 * additional entities must be introduced" requirement).
 */
public abstract class EntityPool<T> {

	private final List<T> pool = new ArrayList<>();
	private final Map<T, Integer> usageCounts = new HashMap<>();
	private int genericCounter = 0;
	private int maxReuses = Integer.MAX_VALUE;
	private AttachmentStrategy attachmentStrategy = new UniformRandomAttachment();

	public void setMaxReuses(int maxReuses) {
		this.maxReuses = maxReuses;
	}

	public void setAttachmentStrategy(AttachmentStrategy attachmentStrategy) {
		this.attachmentStrategy = attachmentStrategy;
	}

	/** Creates and registers a new entity with a generic name, without marking it as used. */
	public T mintNew() {
		T entity = createNamed(namePrefix() + (++genericCounter));
		pool.add(entity);
		usageCounts.put(entity, 0);
		return entity;
	}

	/**
	 * Returns an entity from the pool (reusing one under {@code maxReuses} if available, otherwise minting a
	 * new one) and marks it as used once more. Which reusable entity gets chosen is delegated to the
	 * configured {@link AttachmentStrategy}.
	 */
	public T pick(Random random) {
		List<T> reusable = pool.stream()
				.filter(e -> usageCounts.getOrDefault(e, 0) < maxReuses)
				.toList();
		T chosen = reusable.isEmpty() ? mintNew() : attachmentStrategy.choose(reusable, e -> usageCounts.getOrDefault(e, 0), random);
		usageCounts.merge(chosen, 1, Integer::sum);
		return chosen;
	}

	/**
	 * Delegates to this pool's configured {@link AttachmentStrategy} to choose among an externally-filtered
	 * candidate subset (e.g. a hierarchy strategy's depth/branching/cycle-eligible superclasses), using the
	 * same usage-count signal {@link #pick} does, rather than {@code pick}'s own {@code maxReuses}-filtered
	 * view of the whole pool. Marks the chosen entity used, same as {@code pick}, so repeated choices here
	 * feed back into future attachment decisions (both here and in ordinary {@code pick} calls) exactly as if
	 * they had gone through {@code pick} directly.
	 */
	public T chooseAmong(List<T> candidates, Random random) {
		T chosen = attachmentStrategy.choose(candidates, e -> usageCounts.getOrDefault(e, 0), random);
		usageCounts.merge(chosen, 1, Integer::sum);
		return chosen;
	}

	/**
	 * Returns an entity from the pool distinct from {@code exclude}, minting one if necessary. A deterministic
	 * {@link AttachmentStrategy} (e.g. {@code ChainAttachment}, which has no randomness at all) can return the
	 * exact same entity on every retry within the attempt budget — falling through to {@code mintNew()} in
	 * that case is required, not just a nicety, since otherwise this method could silently violate its own
	 * contract and return an entity equal to {@code exclude}.
	 */
	public T pickDifferentFrom(Random random, T exclude) {
		if (pool.size() <= 1 && pool.contains(exclude)) {
			return mintNew();
		}
		T chosen;
		int attempts = 0;
		do {
			chosen = pick(random);
			attempts++;
		} while (chosen.equals(exclude) && attempts < 8);
		return chosen.equals(exclude) ? mintNew() : chosen;
	}

	public List<T> all() {
		return List.copyOf(pool);
	}

	public int size() {
		return pool.size();
	}

	protected abstract T createNamed(String name);

	protected abstract String namePrefix();
}
