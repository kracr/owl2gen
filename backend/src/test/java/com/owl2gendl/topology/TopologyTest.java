package com.owl2gendl.topology;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.generator.classaxiom.SubClassOfGenerator;
import com.owl2gendl.orchestration.EntityProvisioningService;

@SpringBootTest
class TopologyTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private SubClassOfGenerator subClassOfGenerator;

	@Test
	void hierarchyStrategyKeepsAchievedDepthNearTarget() {
		assertDepthNearTarget(TopologyVariant.UNIFORM_RANDOM, 1L);
	}

	/**
	 * Regression test for a bug where {@code DepthBiasedHierarchyStrategy} picked uniformly at random among
	 * eligible superclasses regardless of the request's topology variant, so only Uniform Random's achieved
	 * depth ever matched a target-depth bound — Chain, Balanced Tree, and Preferential all silently ignored
	 * the variant for hierarchy shape specifically (they still respected it for which class plays the
	 * subclass role), which let achieved depth run far past the target. Superclass choice among eligible
	 * candidates is now delegated to the pool's configured {@link AttachmentStrategy}
	 * ({@link com.owl2gendl.pool.EntityPool#chooseAmong}), the same one every other construct already uses,
	 * and a separate bug in {@link BalancedTreeAttachment} (it never used its {@code Random} parameter,
	 * always breaking usage-count ties by picking the first candidate) is fixed alongside it: deterministic
	 * tie-breaking combined with cycle-avoidance was collapsing entire pools into one linear chain.
	 *
	 * <p>Uses a realistic pool-to-request ratio (matching the real-ontology-derived request that first
	 * surfaced this: 100 classes, 259 requested {@code SubClassOf} axioms, target depth 6) rather than the
	 * artificially tiny 5-class pool the cycle-avoidance test below uses — at that extreme a ratio, even a
	 * correctly-implemented depth-biased strategy can be forced into long fallback chains once the whole pool
	 * cycle-saturates, which is a separate, more fundamental small-pool characteristic than this bug. The
	 * bound below (3x target) is deliberately generous — it is a regression guard against collapsing back
	 * toward the originally observed 259-vs-6 order-of-magnitude blowout, not a claim that depth bias is
	 * exact (the existing thesis text already documents depth/branching as bias targets, not guarantees).
	 */
	@Test
	void hierarchyStrategyKeepsAchievedDepthNearTargetForEveryVariant() {
		assertDepthNearTarget(TopologyVariant.CHAIN, 6L);
		assertDepthNearTarget(TopologyVariant.BALANCED_TREE, 7L);
		assertDepthNearTarget(TopologyVariant.PREFERENTIAL, 8L);
	}

	private void assertDepthNearTarget(TopologyVariant variant, long seed) {
		int targetDepth = 6;
		TopologyConfig config = new TopologyConfig(variant, targetDepth, 4, 0, 0.0);
		GenerationContext ctx = contextFactory.create(seed, config);
		provisioningService.seed(ctx, new EntityCountsDto(100, 0, 0, 0));

		List<OWLAxiom> axioms = subClassOfGenerator.generate(ctx, 259);

		Map<OWLClass, OWLClass> parentOf = new HashMap<>();
		for (OWLAxiom axiom : axioms) {
			OWLSubClassOfAxiom subClassOf = (OWLSubClassOfAxiom) axiom;
			parentOf.put(subClassOf.getSubClass().asOWLClass(), subClassOf.getSuperClass().asOWLClass());
		}

		int maxDepth = 0;
		for (OWLClass leaf : parentOf.keySet()) {
			int depth = 0;
			OWLClass current = leaf;
			Set<OWLClass> visited = new HashSet<>();
			while (parentOf.containsKey(current) && visited.add(current) && depth < 300) {
				current = parentOf.get(current);
				depth++;
			}
			maxDepth = Math.max(maxDepth, depth);
		}

		assertTrue(maxDepth <= targetDepth * 3,
				variant + ": achieved depth " + maxDepth + " far exceeds target " + targetDepth);
	}

	@Test
	void hierarchyStrategyNeverProducesASubClassOfCycle() {
		// A small class pool with far more requested axioms than classes forces heavy reuse of the same
		// classes as both subclass and superclass across different axioms — exactly the condition that
		// used to let a reused class be picked as its own (transitive) superclass, closing a cycle.
		TopologyConfig config = new TopologyConfig(TopologyVariant.UNIFORM_RANDOM, 8, 3, 0, 0.0);
		GenerationContext ctx = contextFactory.create(5L, config);
		provisioningService.seed(ctx, new EntityCountsDto(15, 0, 0, 0));

		List<OWLAxiom> axioms = subClassOfGenerator.generate(ctx, 80);

		Map<OWLClass, List<OWLClass>> parentsOf = new HashMap<>();
		for (OWLAxiom axiom : axioms) {
			OWLSubClassOfAxiom subClassOf = (OWLSubClassOfAxiom) axiom;
			parentsOf.computeIfAbsent(subClassOf.getSubClass().asOWLClass(), k -> new java.util.ArrayList<>())
					.add(subClassOf.getSuperClass().asOWLClass());
		}

		for (OWLClass start : parentsOf.keySet()) {
			assertFalse(reachesItself(start, parentsOf), "cycle detected reachable from " + start);
		}
	}

	private boolean reachesItself(OWLClass start, Map<OWLClass, List<OWLClass>> parentsOf) {
		Set<OWLClass> visited = new HashSet<>();
		java.util.Deque<OWLClass> toVisit = new java.util.ArrayDeque<>(parentsOf.getOrDefault(start, List.of()));
		while (!toVisit.isEmpty()) {
			OWLClass current = toVisit.pop();
			if (current.equals(start)) {
				return true;
			}
			if (!visited.add(current)) {
				continue;
			}
			toVisit.addAll(parentsOf.getOrDefault(current, List.of()));
		}
		return false;
	}

	@Test
	void nestingProbabilityOneAlwaysProducesACompoundFillerAtTheTopLevel() {
		TopologyConfig config = new TopologyConfig(TopologyVariant.UNIFORM_RANDOM, 4, 3, 2, 1.0);
		GenerationContext ctx = contextFactory.create(2L, config);
		provisioningService.seed(ctx, new EntityCountsDto(5, 0, 0, 0));

		for (int i = 0; i < 10; i++) {
			OWLClassExpression filler = ctx.nestingStrategy().buildClassFiller(ctx, 0);
			assertFalse(filler.isOWLClass(), "expected a nested compound filler, got a plain class: " + filler);
		}
	}

	@Test
	void nestingProbabilityZeroNeverProducesACompoundFiller() {
		TopologyConfig config = new TopologyConfig(TopologyVariant.UNIFORM_RANDOM, 4, 3, 2, 0.0);
		GenerationContext ctx = contextFactory.create(3L, config);
		provisioningService.seed(ctx, new EntityCountsDto(5, 0, 0, 0));

		for (int i = 0; i < 10; i++) {
			OWLClassExpression filler = ctx.nestingStrategy().buildClassFiller(ctx, 0);
			assertTrue(filler.isOWLClass(), "expected a plain class filler, got a nested expression: " + filler);
		}
	}

	@Test
	void chainAttachmentConcentratesReuseOnFewEntitiesMoreThanUniformRandom() {
		int maxUsageUnderChain = maxClassUsageCount(TopologyVariant.CHAIN);
		int maxUsageUnderUniform = maxClassUsageCount(TopologyVariant.UNIFORM_RANDOM);

		assertTrue(maxUsageUnderChain >= maxUsageUnderUniform,
				"chain attachment (max usage " + maxUsageUnderChain + ") should concentrate reuse at least as "
						+ "much as uniform random (max usage " + maxUsageUnderUniform + ")");
	}

	private int maxClassUsageCount(TopologyVariant variant) {
		TopologyConfig config = new TopologyConfig(variant, 10, 10, 0, 0.0);
		GenerationContext ctx = contextFactory.create(4L, config);
		provisioningService.seed(ctx, new EntityCountsDto(5, 0, 0, 0));

		Map<OWLClass, Integer> usage = new HashMap<>();
		for (int i = 0; i < 40; i++) {
			OWLClass picked = ctx.pools().classes().pick(ctx.random());
			usage.merge(picked, 1, Integer::sum);
		}
		return usage.values().stream().mapToInt(Integer::intValue).max().orElse(0);
	}
}
