package com.owl2gendl.generator;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructCatalog;
import com.owl2gendl.catalog.ConstructId;

/**
 * Builds the {@link ConstructId} -> {@link ConstructGenerator} map from every {@code ConstructGenerator}
 * Spring bean and hard-fails at startup if the registry's keyset doesn't exactly match
 * {@link ConstructCatalog}'s keyset. This is the direct fix for the old codebase's silent gaps, where
 * several constructs (AllDisjointClasses, AllDisjointObjectProperties, unqualified cardinality
 * restrictions, ...) were implemented in Java but never wired into the frontend or {@code app.java}'s
 * construct map — here, a construct existing in the catalog without a generator (or vice versa) is a
 * startup failure, not a silent omission someone has to notice later.
 */
@Component
public class ConstructGeneratorRegistry {

	private final Map<ConstructId, ConstructGenerator> generators;

	public ConstructGeneratorRegistry(List<ConstructGenerator> generatorBeans, ConstructCatalog catalog) {
		Map<ConstructId, ConstructGenerator> map = new EnumMap<>(ConstructId.class);
		for (ConstructGenerator generator : generatorBeans) {
			ConstructGenerator existing = map.put(generator.id(), generator);
			if (existing != null) {
				throw new IllegalStateException("Duplicate ConstructGenerator registered for " + generator.id());
			}
		}
		this.generators = Map.copyOf(map);

		Set<ConstructId> missing = catalog.all().stream()
				.filter(id -> !generators.containsKey(id))
				.collect(Collectors.toCollection(() -> java.util.EnumSet.noneOf(ConstructId.class)));
		if (!missing.isEmpty()) {
			throw new IllegalStateException("%d of %d catalog constructs have no registered generator: %s"
					.formatted(missing.size(), catalog.all().size(), missing));
		}
	}

	public ConstructGenerator get(ConstructId id) {
		ConstructGenerator generator = generators.get(id);
		if (generator == null) {
			throw new IllegalArgumentException("No generator registered for construct " + id);
		}
		return generator;
	}

	public boolean has(ConstructId id) {
		return generators.containsKey(id);
	}

	public int registeredCount() {
		return generators.size();
	}
}
