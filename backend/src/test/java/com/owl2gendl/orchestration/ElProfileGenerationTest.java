package com.owl2gendl.orchestration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.reasoning.TargetProfile;
import com.owl2gendl.topology.TopologyConfig;
import com.owl2gendl.topology.TopologyVariant;

/**
 * End-to-end check that a real, multi-construct, nesting-enabled EL-targeted generation run actually
 * produces an EL-compliant ontology on the live in-memory {@code ctx.ontology()} — not just that each
 * construct is individually EL-legal in isolation ({@code ConstructProfileEligibilityTest}), which doesn't
 * catch interactions between constructs, entity reuse, and nesting.
 */
@SpringBootTest
class ElProfileGenerationTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private OntologyGenerationService generationService;

	@Test
	void aRealElTargetedRequestWithNestingProducesAnElCompliantOntology() {
		TopologyConfig config = new TopologyConfig(TopologyVariant.UNIFORM_RANDOM, 4, 3, 2, 0.5);
		GenerationContext ctx = contextFactory.create(42L, config, TargetProfile.EL);
		provisioningService.seed(ctx, new EntityCountsDto(20, 5, 2, 5));

		Map<ConstructId, Integer> requested = new EnumMap<>(ConstructId.class);
		requested.put(ConstructId.SUB_CLASS_OF, 15);
		requested.put(ConstructId.OBJECT_SOME_VALUES_FROM, 10);
		requested.put(ConstructId.OBJECT_INTERSECTION_OF, 5);
		generationService.generate(UUID.randomUUID(), ctx, requested, false);

		OWLProfileReport report = new OWL2ELProfile().checkOntology(ctx.ontology());
		assertTrue(report.isInProfile(), () -> "Expected EL compliance but got violations: " + report.getViolations());
	}
}
