package com.owl2gendl.generation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructCategory;
import com.owl2gendl.catalog.ConstructId;

/**
 * Sequences the requested construct types before generation, per {@link ConstructOrderMode}. Only affects
 * the order construct-type batches are generated and (under {@code strictConsistency}) checked in — it does
 * not change which constructs are requested or how many of each, and under the non-strict (fast) path it has
 * no observable effect at all, since every construct type's axioms are accumulated before a single final
 * check regardless of order.
 *
 * <p>{@code DEFAULT} is deliberately a no-op: {@link ConstructId} declaration order, exactly what every
 * request has always used ({@code OntologyGenerationService}'s own class-level doc records that a curated
 * reordering was tried under {@code strictConsistency} and reverted after it made cumulative reasoning cost
 * worse, not better — see that class for the mechanism). {@code CATEGORY}, {@code RANDOM}, and {@code CUSTOM}
 * are opt-in: a deployment or request can set them to explore whether some other order suits its own
 * construct mix, without this project asserting any particular order is an improvement.
 */
@Component
public class ConstructOrderPlanner {

	private static final List<ConstructId> DECLARATION_ORDER = List.of(ConstructId.values());

	private final ConstructOrderMode mode;
	private final List<ConstructCategory> categoryOrder;
	private final List<ConstructId> customOrder;

	public ConstructOrderPlanner(
			@Value("${owl2gendl.generation.construct-order-mode:DEFAULT}") ConstructOrderMode mode,
			@Value("#{'${owl2gendl.generation.category-order:CLASS_AXIOM,OBJECT_PROPERTY_AXIOM,DATA_PROPERTY_AXIOM,ASSERTION,CLASS_EXPRESSION,OBJECT_PROPERTY_RESTRICTION,DATA_PROPERTY_RESTRICTION,DATA_RANGE}'.split(',')}")
			List<String> categoryOrder,
			@Value("#{'${owl2gendl.generation.custom-construct-order:}'.split(',')}") List<String> customOrder) {
		this.mode = mode;
		this.categoryOrder = categoryOrder.stream().map(String::trim).filter(s -> !s.isEmpty())
				.map(ConstructCategory::valueOf).toList();
		this.customOrder = customOrder.stream().map(String::trim).filter(s -> !s.isEmpty())
				.map(ConstructId::valueOf).toList();
	}

	/** @return the requested construct types (only those with a positive count), in the configured order. */
	public List<ConstructId> order(Map<ConstructId, Integer> requestedCounts, Random random) {
		List<ConstructId> requested = requestedCounts.entrySet().stream()
				.filter(e -> e.getValue() != null && e.getValue() > 0)
				.map(Map.Entry::getKey)
				.toList();

		return switch (mode) {
			case DEFAULT -> declarationOrderOf(requested);
			case CATEGORY -> categoryOrderOf(requested);
			case RANDOM -> randomOrderOf(requested, random);
			case CUSTOM -> customOrderOf(requested);
		};
	}

	private List<ConstructId> declarationOrderOf(List<ConstructId> requested) {
		Set<ConstructId> requestedSet = new LinkedHashSet<>(requested);
		return DECLARATION_ORDER.stream().filter(requestedSet::contains).toList();
	}

	private List<ConstructId> categoryOrderOf(List<ConstructId> requested) {
		Set<ConstructId> requestedSet = new LinkedHashSet<>(requested);
		List<ConstructId> ordered = new ArrayList<>();
		for (ConstructCategory category : categoryOrder) {
			for (ConstructId id : DECLARATION_ORDER) {
				if (id.category() == category && requestedSet.contains(id)) {
					ordered.add(id);
				}
			}
		}
		// Any category not mentioned in categoryOrder still gets processed, appended afterward, so a
		// misconfigured (incomplete) category-order never silently drops a requested construct type.
		for (ConstructId id : DECLARATION_ORDER) {
			if (requestedSet.contains(id) && !ordered.contains(id)) {
				ordered.add(id);
			}
		}
		return ordered;
	}

	private List<ConstructId> randomOrderOf(List<ConstructId> requested, Random random) {
		List<ConstructId> shuffled = new ArrayList<>(requested);
		Collections.shuffle(shuffled, random);
		return shuffled;
	}

	private List<ConstructId> customOrderOf(List<ConstructId> requested) {
		Set<ConstructId> requestedSet = new LinkedHashSet<>(requested);
		List<ConstructId> ordered = new ArrayList<>();
		for (ConstructId id : customOrder) {
			if (requestedSet.contains(id) && !ordered.contains(id)) {
				ordered.add(id);
			}
		}
		// Requested constructs the custom order didn't mention: appended in declaration order, not dropped.
		for (ConstructId id : DECLARATION_ORDER) {
			if (requestedSet.contains(id) && !ordered.contains(id)) {
				ordered.add(id);
			}
		}
		return ordered;
	}
}
