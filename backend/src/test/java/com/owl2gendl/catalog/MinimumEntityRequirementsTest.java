package com.owl2gendl.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinimumEntityRequirementsTest {

	@Test
	void everyConstructHasANonNegativeRequirement() {
		for (ConstructId id : ConstructId.values()) {
			EntityRequirement requirement = MinimumEntityRequirements.forConstruct(id);
			assertTrue(requirement.classes() >= 0, id + " classes");
			assertTrue(requirement.objectProperties() >= 0, id + " objectProperties");
			assertTrue(requirement.dataProperties() >= 0, id + " dataProperties");
			assertTrue(requirement.individuals() >= 0, id + " individuals");
		}
	}

	@Test
	void nAryDisjointnessConstructsNeedThree() {
		assertEquals(3, MinimumEntityRequirements.forConstruct(ConstructId.ALL_DISJOINT_CLASSES).classes());
		assertEquals(3, MinimumEntityRequirements.forConstruct(ConstructId.DISJOINT_UNION).classes());
		assertEquals(3, MinimumEntityRequirements.forConstruct(ConstructId.ALL_DISJOINT_OBJECT_PROPERTIES).objectProperties());
		assertEquals(3, MinimumEntityRequirements.forConstruct(ConstructId.ALL_DISJOINT_DATA_PROPERTIES).dataProperties());
	}

	@Test
	void pairwiseConstructsNeedTwo() {
		assertEquals(2, MinimumEntityRequirements.forConstruct(ConstructId.SUB_CLASS_OF).classes());
		assertEquals(2, MinimumEntityRequirements.forConstruct(ConstructId.INVERSE_OF_PROPERTY).objectProperties());
		assertEquals(2, MinimumEntityRequirements.forConstruct(ConstructId.SUB_DATA_PROPERTY_OF).dataProperties());
	}

	@Test
	void forSelectionTakesTheMaxAcrossAllSelectedConstructs() {
		EntityRequirement combined = MinimumEntityRequirements.forSelection(
				java.util.List.of(ConstructId.SUB_CLASS_OF, ConstructId.ALL_DISJOINT_CLASSES));
		assertEquals(3, combined.classes(), "the stricter of the two selected constructs' requirements must win");
	}
}
