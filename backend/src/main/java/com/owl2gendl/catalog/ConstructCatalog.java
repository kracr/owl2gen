package com.owl2gendl.catalog;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * Groups every {@link ConstructId} by its {@link ConstructCategory}, in enum declaration order. This is the
 * data driving {@code GET /api/catalog} and, transitively, the entire construct-picker UI on the frontend —
 * there must be no construct list hardcoded anywhere else.
 */
@Component
public class ConstructCatalog {

	private final Map<ConstructCategory, List<ConstructId>> byCategory;

	public ConstructCatalog() {
		Map<ConstructCategory, List<ConstructId>> grouped = new EnumMap<>(ConstructCategory.class);
		for (ConstructCategory category : ConstructCategory.values()) {
			grouped.put(category, Arrays.stream(ConstructId.values())
					.filter(id -> id.category() == category)
					.collect(Collectors.toUnmodifiableList()));
		}
		this.byCategory = Map.copyOf(grouped);
	}

	public Map<ConstructCategory, List<ConstructId>> byCategory() {
		return byCategory;
	}

	public List<ConstructId> all() {
		return List.of(ConstructId.values());
	}
}
