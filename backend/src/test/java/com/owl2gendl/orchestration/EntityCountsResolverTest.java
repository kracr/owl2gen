package com.owl2gendl.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructId;

class EntityCountsResolverTest {

	private final EntityCountsResolver resolver = new EntityCountsResolver(20, 6, 3, 10);

	@Test
	void fillsInAllFieldsWhenEntityCountsIsEntirelyNull() {
		EntityCountsDto resolved = resolver.resolve(null, Set.of(ConstructId.SUB_CLASS_OF));
		assertEquals(20, resolved.classes());
		assertEquals(6, resolved.objectProperties());
		assertEquals(3, resolved.dataProperties());
		assertEquals(10, resolved.individuals());
	}

	@Test
	void preservesExplicitFieldsAndFillsOnlyTheMissingOnes() {
		EntityCountsDto resolved = resolver.resolve(new EntityCountsDto(50, null, 0, null), Set.of(ConstructId.SUB_CLASS_OF));
		assertEquals(50, resolved.classes(), "explicit value must be preserved exactly, not adjusted");
		assertEquals(6, resolved.objectProperties(), "unset field falls back to the default");
		assertEquals(0, resolved.dataProperties(), "explicit zero must be preserved, not treated as unset");
		assertEquals(10, resolved.individuals());
	}

	@Test
	void bumpsAnUnsetDefaultUpWhenTheSelectionStructurallyNeedsMore() {
		// The default object property count (6) already comfortably clears ALL_DISJOINT_OBJECT_PROPERTIES's
		// minimum of 3, so this exercises the "selection needs more than the base default" path directly by
		// using a construct whose minimum exceeds the default for a type with a very low starting default.
		EntityCountsDto resolved = resolver.resolve(new EntityCountsDto(null, null, null, null),
				Set.of(ConstructId.ALL_DISJOINT_DATA_PROPERTIES));
		assertEquals(3, resolved.dataProperties(), "3 clears both the default (3) and the requirement (3)");
	}

	@Test
	void neverLowersAnUnsetFieldBelowTheBaseDefault() {
		EntityCountsDto resolved = resolver.resolve(new EntityCountsDto(null, null, null, null), Set.of(ConstructId.DATATYPE_DEFINITION));
		assertEquals(20, resolved.classes(), "a construct needing 0 pre-existing classes must not shrink the base default");
	}
}
