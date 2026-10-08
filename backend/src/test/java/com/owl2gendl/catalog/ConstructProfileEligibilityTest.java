package com.owl2gendl.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.orchestration.EntityProvisioningService;
import com.owl2gendl.orchestration.OntologyGenerationService;
import com.owl2gendl.topology.TopologyConfig;
import com.owl2gendl.topology.TopologyVariant;

/**
 * {@link ConstructId#elEligible()} is a hand-set classification, not the source of truth. This test is the
 * source of truth: for every construct, generate one real sample axiom via its actual registered generator
 * and check it against OWL API's own {@link OWL2ELProfile} checker — the authoritative implementation, not
 * anyone's memory of the OWL 2 EL specification. Any mismatch here means the hand-set flag is wrong and must
 * be corrected, exactly like {@code ConstructGeneratorRegistry}'s startup completeness check guards the
 * catalog/generator keyset elsewhere in this codebase.
 *
 * <p>Nesting probability is forced to zero here deliberately: nesting's own EL-safety (whether it only ever
 * builds EL-legal fillers when a profile is requested) is a separate concern, verified elsewhere. This test's
 * job is narrowly "is this construct's own axiom shape EL-legal", not "does nesting behave" — mixing the two
 * would make results flaky by the luck of the random nesting draw rather than a real property of the construct.
 */
@SpringBootTest
class ConstructProfileEligibilityTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private OntologyGenerationService generationService;

	@ParameterizedTest
	@EnumSource(ConstructId.class)
	void elEligibleFlagMatchesTheAuthoritativeOwlApiElProfileChecker(ConstructId constructId) {
		TopologyConfig noNesting = new TopologyConfig(TopologyVariant.UNIFORM_RANDOM, 4, 3, 0, 0.0);
		GenerationContext ctx = contextFactory.create(11L, noNesting);
		// Generous seeding, same as AllConstructsCoverageTest, so every construct (including ones needing
		// individuals or several distinct entities) has enough to generate a representative sample from.
		provisioningService.seed(ctx, new EntityCountsDto(8, 5, 5, 8));

		Map<ConstructId, Integer> requested = new EnumMap<>(ConstructId.class);
		requested.put(constructId, 1);
		generationService.generate(UUID.randomUUID(), ctx, requested, false);

		boolean actuallyElCompliant = new OWL2ELProfile().checkOntology(ctx.ontology()).isInProfile();

		assertEquals(constructId.elEligible(), actuallyElCompliant, () -> constructId
				+ ": ConstructId.elEligible() says " + constructId.elEligible()
				+ " but OWL API's OWL2ELProfile checker says " + actuallyElCompliant
				+ " for a real generated sample axiom");
	}
}
