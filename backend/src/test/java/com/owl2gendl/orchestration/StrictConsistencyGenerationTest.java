package com.owl2gendl.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.reasoning.VerificationService;

/**
 * Exercises the {@code strictConsistency} retry-then-drop mechanism against a deterministic, guaranteed
 * contradiction (rather than trying to reproduce one of the genuinely subtle multi-hop entailments found in
 * production, which depend on random draws) - keeps the test fast and reliable while still proving the
 * mechanism end to end: a batch that provably can't be made consistent gets dropped, and the ontology that
 * remains is left consistent.
 */
@SpringBootTest
class StrictConsistencyGenerationTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private OntologyGenerationService generationService;
	@Autowired
	private VerificationService verificationService;

	@Test
	void dropsAConstructTypeThatCanNeverProduceAConsistentBatchAndLeavesTheRestConsistent() {
		GenerationContext ctx = contextFactory.create(1L);
		// Exactly one class and one individual - CLASS_ASSERTION has no choice but to produce
		// ClassAssertion(theOnlyClass, theOnlyIndividual) on every attempt, deterministically, regardless of seed.
		provisioningService.seed(ctx, new EntityCountsDto(1, 0, 0, 1));
		OWLClass theOnlyClass = ctx.pools().classes().all().get(0);

		// Directly make that one class unconditionally unsatisfiable (SubClassOf(C, owl:Nothing)) - on its
		// own this doesn't make the ontology inconsistent (an empty class is fine until something is asserted
		// into it), so the ontology is consistent *before* CLASS_ASSERTION runs.
		ctx.ontologyManager().addAxiom(ctx.ontology(), ctx.dataFactory().getOWLSubClassOfAxiom(theOnlyClass, ctx.dataFactory().getOWLNothing()));
		assertTrue(verificationService.checkConsistency(ctx.ontology(), Duration.ofSeconds(10)),
				"sanity check: an unsatisfiable-but-uninstantiated class must not itself make the ontology inconsistent");

		Map<ConstructId, Integer> requested = new EnumMap<>(ConstructId.class);
		requested.put(ConstructId.CLASS_ASSERTION, 1);

		Set<ConstructId> dropped = generationService.generate(UUID.randomUUID(), ctx, requested, true);

		assertEquals(Set.of(ConstructId.CLASS_ASSERTION), dropped,
				"ClassAssertion(theOnlyClass, theOnlyIndividual) is unconditionally inconsistent here, so it must be dropped after retries");
		assertFalse(ctx.ontology().containsAxiom(
				ctx.dataFactory().getOWLClassAssertionAxiom(theOnlyClass, ctx.pools().individuals().all().get(0))),
				"the rolled-back batch's axiom must not remain in the ontology");
		assertTrue(verificationService.checkConsistency(ctx.ontology(), Duration.ofSeconds(10)),
				"after dropping the only construct that could never be made consistent, what remains must be consistent");
	}

	@Test
	void strictConsistencyDoesNotAffectAnOrdinaryConsistentRequest() {
		GenerationContext ctx = contextFactory.create(2L);
		provisioningService.seed(ctx, new EntityCountsDto(20, 6, 3, 10));

		Map<ConstructId, Integer> requested = new EnumMap<>(ConstructId.class);
		requested.put(ConstructId.SUB_CLASS_OF, 10);
		requested.put(ConstructId.DISJOINT_WITH, 3);

		Set<ConstructId> dropped = generationService.generate(UUID.randomUUID(), ctx, requested, true);

		assertTrue(dropped.isEmpty(), "an ordinary, easily-satisfiable request should never need to drop anything");
		assertTrue(verificationService.checkConsistency(ctx.ontology(), Duration.ofSeconds(10)));
	}
}
