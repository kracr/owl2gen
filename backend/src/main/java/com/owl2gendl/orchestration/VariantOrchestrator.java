package com.owl2gendl.orchestration;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.owl2gendl.api.dto.GenerationRequestDto;
import com.owl2gendl.api.dto.ReasoningConfigDto;
import com.owl2gendl.api.dto.StructureConfigDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.job.VariantOutcome;
import com.owl2gendl.metrics.MetricsReport;
import com.owl2gendl.metrics.OntologyMetricsService;
import com.owl2gendl.reasoning.ReasonerTier;
import com.owl2gendl.reasoning.TargetProfile;
import com.owl2gendl.reasoning.VerificationResult;
import com.owl2gendl.reasoning.VerificationService;
import com.owl2gendl.reasoning.VerificationStatus;
import com.owl2gendl.topology.TopologyConfig;
import com.owl2gendl.topology.TopologyVariant;

/**
 * Runs one full generate-verify-measure pipeline for a single {@link TopologyVariant}, given a shared
 * request. {@code GenerationJobRunner} calls this once per requested variant.
 */
@Service
public class VariantOrchestrator {

	private static final Logger log = LoggerFactory.getLogger(VariantOrchestrator.class);

	private final GenerationContextFactory contextFactory;
	private final EntityProvisioningService provisioningService;
	private final OntologyGenerationService generationService;
	private final VerificationService verificationService;
	private final OntologyMetricsService metricsService;
	private final int defaultTimeoutSeconds;

	/**
	 * Bounded retry on a genuinely INCONSISTENT result: regenerate the whole variant from a different seed
	 * before accepting it, mirroring this project's existing honesty principle for reasoning timeouts
	 * (report NOT_VERIFIED_TIMEOUT rather than pretend) — try harder, then report the true final outcome.
	 * NOT_VERIFIED_TIMEOUT/NOT_VERIFIED_ERROR are deliberately not retried this way: a timeout is about the
	 * ontology's size/complexity, not a bad random draw, so retrying just spends the time budget 3x for
	 * nothing.
	 */
	private final int maxInconsistentAttempts;

	private final int defaultHierarchyTargetDepth;
	private final int defaultHierarchyBranchingFactor;
	private final int defaultNestingMaxDepth;
	private final double defaultNestingProbability;

	public VariantOrchestrator(GenerationContextFactory contextFactory, EntityProvisioningService provisioningService,
			OntologyGenerationService generationService, VerificationService verificationService,
			OntologyMetricsService metricsService,
			@Value("${owl2gendl.generation.default-final-verification-timeout-seconds:30}") int defaultTimeoutSeconds,
			@Value("${owl2gendl.generation.max-inconsistent-attempts:3}") int maxInconsistentAttempts,
			@Value("${owl2gendl.generation.default-hierarchy-target-depth:4}") int defaultHierarchyTargetDepth,
			@Value("${owl2gendl.generation.default-hierarchy-branching-factor:3}") int defaultHierarchyBranchingFactor,
			@Value("${owl2gendl.generation.default-nesting-max-depth:2}") int defaultNestingMaxDepth,
			@Value("${owl2gendl.generation.default-nesting-probability:0.3}") double defaultNestingProbability) {
		this.contextFactory = contextFactory;
		this.provisioningService = provisioningService;
		this.generationService = generationService;
		this.verificationService = verificationService;
		this.metricsService = metricsService;
		this.defaultTimeoutSeconds = defaultTimeoutSeconds;
		this.maxInconsistentAttempts = maxInconsistentAttempts;
		this.defaultHierarchyTargetDepth = defaultHierarchyTargetDepth;
		this.defaultHierarchyBranchingFactor = defaultHierarchyBranchingFactor;
		this.defaultNestingMaxDepth = defaultNestingMaxDepth;
		this.defaultNestingProbability = defaultNestingProbability;
	}

	public void run(UUID jobId, TopologyVariant variant, GenerationRequestDto request, VariantOutcome outcome) {
		outcome.markRunning();

		TargetProfile targetProfile = TargetProfile.resolve(request.targetProfile());
		TopologyConfig config = toTopologyConfig(variant, request.structure());
		Duration timeout = Duration.ofSeconds(timeoutSeconds(request.reasoning()));

		Map<ConstructId, Integer> requestedCounts = new EnumMap<>(ConstructId.class);
		request.constructs().forEach((name, count) -> requestedCounts.put(ConstructId.valueOf(name), count));

		GenerationContext ctx = null;
		VerificationResult verification = null;
		for (int attempt = 0; attempt < maxInconsistentAttempts; attempt++) {
			// Offset (not reuse) the seed per attempt: retrying with the exact same seed would deterministically
			// reproduce the same inconsistent ontology every time, defeating the point of retrying.
			Long attemptSeed = request.seed() != null ? request.seed() + attempt : null;
			ctx = contextFactory.create(attemptSeed, config, targetProfile);
			provisioningService.seed(ctx, request.entityCounts());
			Set<ConstructId> dropped = generationService.generate(jobId, ctx, requestedCounts, Boolean.TRUE.equals(request.strictConsistency()));
			if (!dropped.isEmpty()) {
				log.warn("Dropped {} construct type(s) that couldn't produce a consistent batch after retries: {}",
						dropped.size(), dropped);
			}

			verification = verificationService.verify(ctx.ontology(), reasonerTierOverride(request.reasoning()), timeout, targetProfile);
			if (verification.status() != VerificationStatus.INCONSISTENT) {
				break;
			}
		}

		MetricsReport metrics = metricsService.compute(ctx.ontology(), timeout);
		outcome.markCompleted(ctx.ontology(), verification, metrics);
	}

	private TopologyConfig toTopologyConfig(TopologyVariant variant, StructureConfigDto structure) {
		if (structure == null) {
			return new TopologyConfig(variant, defaultHierarchyTargetDepth, defaultHierarchyBranchingFactor,
					defaultNestingMaxDepth, defaultNestingProbability);
		}
		return new TopologyConfig(
				variant,
				structure.hierarchyTargetDepth() != null ? structure.hierarchyTargetDepth() : defaultHierarchyTargetDepth,
				structure.hierarchyBranchingFactor() != null ? structure.hierarchyBranchingFactor() : defaultHierarchyBranchingFactor,
				structure.nestingMaxDepth() != null ? structure.nestingMaxDepth() : defaultNestingMaxDepth,
				structure.nestingProbability() != null ? structure.nestingProbability() : defaultNestingProbability);
	}

	private int timeoutSeconds(ReasoningConfigDto reasoning) {
		if (reasoning == null || reasoning.timeoutSeconds() == null) {
			return defaultTimeoutSeconds;
		}
		return reasoning.timeoutSeconds();
	}

	private ReasonerTier reasonerTierOverride(ReasoningConfigDto reasoning) {
		if (reasoning == null || reasoning.tier() == null) {
			return null;
		}
		return ReasonerTier.valueOf(reasoning.tier());
	}
}
