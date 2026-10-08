package com.owl2gendl.orchestration;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generation.ConstructOrderPlanner;
import com.owl2gendl.generator.ConstructGeneratorRegistry;
import com.owl2gendl.reasoning.BatchCheckOutcome;
import com.owl2gendl.reasoning.BatchDiagnosticService;
import com.owl2gendl.reasoning.TargetProfile;
import com.owl2gendl.reasoning.VerificationService;

/**
 * Core generation loop: for each requested construct/count, ask the registry for its generator and add the
 * resulting axioms to the ontology.
 *
 * <p>Two modes:
 *
 * <p><b>Fast (default).</b> Every construct type's axioms get generated and added in one pass, no reasoning
 * until the caller's own (single, final) verification step. This is what every request has always done.
 *
 * <p><b>{@code strictConsistency}.</b> After each construct type's batch is added, a quick reasoner check
 * (see {@link VerificationService#checkConsistency}) confirms the ontology is still consistent. If not, the
 * batch is rolled back and that construct type is retried (a fresh call naturally draws different random
 * picks, no reseeding needed) up to {@code maxBatchRetryAttempts} times; if it still can't produce a
 * consistent batch, that construct type is dropped for this attempt and generation continues with the rest.
 * This exists because some inconsistencies are multi-hop logical entailments (through chains of
 * {@code EquivalentClasses}, {@code ObjectPropertyDomain}, existential restrictions, {@code DisjointClasses},
 * ...) that no local, per-axiom-pair syntactic rule can catch - only an actual reasoner can. Opt-in because
 * it trades real generation latency (up to one reasoner call per selected construct type) for a much higher
 * chance of a consistent result.
 *
 * <p>The per-batch timeout and retry count are deployment-level settings, not per-request ones — a fixed
 * budget that works for a modest construct selection can be too tight once a request leans on reasoner-heavy
 * constructs (property chains, cardinality restrictions). Rather than hardcode a single number that can't
 * suit every deployment, both are read from {@code application.yml} (or any Spring property source) with the
 * project's original values as defaults, so an operator can widen them - e.g. via an environment-specific
 * profile - without a code change or recompile.
 *
 * <p>Construct types are generated in the order produced by {@link ConstructOrderPlanner}, which defaults to
 * {@code requestedCounts}' own declaration order — an {@code EnumMap}, so it iterates in {@link ConstructId}
 * declaration order (itself chosen for the UI's construct catalogue, not for generation), exactly as every
 * request has always done. A deliberately reordered, dependency- and reasoner-fragility-aware sequence was
 * tried and measured against this one; it fixed the batch-ordering fairness issue it targeted but introduced
 * a worse regression of its own — reliable final-verification timeouts, at the largest tested request size,
 * for every topology that concentrates entity reuse (Chain, Balanced Tree, Preferential) — because isolating
 * reasoner-fragile constructs (property chains, cardinality restrictions) into early phases to protect their
 * own check left them present for every later check for the rest of generation, raising the cumulative
 * reasoning burden rather than lowering it. That attempt was reverted in favour of this simpler, already
 * load-bearing order; the underlying fairness issue remains a known, disclosed limitation rather than a
 * shipped fix. {@link ConstructOrderPlanner}'s other modes (category/random/custom) are exposed as an opt-in
 * control for a deployment or request to explore on its own construct mix, not as a claimed improvement.
 */
@Service
public class OntologyGenerationService {

	private final int maxBatchRetryAttempts;
	private final Duration intermediateCheckTimeout;

	private final ConstructGeneratorRegistry registry;
	private final VerificationService verificationService;
	private final BatchDiagnosticService diagnosticService;
	private final ConstructOrderPlanner orderPlanner;

	public OntologyGenerationService(ConstructGeneratorRegistry registry, VerificationService verificationService,
			BatchDiagnosticService diagnosticService, ConstructOrderPlanner orderPlanner,
			@Value("${owl2gendl.generation.max-batch-retry-attempts:3}") int maxBatchRetryAttempts,
			@Value("${owl2gendl.generation.intermediate-check-timeout-seconds:3}") long intermediateCheckTimeoutSeconds) {
		this.registry = registry;
		this.verificationService = verificationService;
		this.diagnosticService = diagnosticService;
		this.orderPlanner = orderPlanner;
		this.maxBatchRetryAttempts = maxBatchRetryAttempts;
		this.intermediateCheckTimeout = Duration.ofSeconds(intermediateCheckTimeoutSeconds);
	}

	/** @return the construct types dropped because no consistent batch could be produced for them (always
	 *  empty when {@code strictConsistency} is {@code false}). */
	public Set<ConstructId> generate(UUID jobId, GenerationContext ctx, Map<ConstructId, Integer> requestedCounts,
			boolean strictConsistency) {
		Set<ConstructId> dropped = strictConsistency
				? generateWithConsistencyChecking(jobId, ctx, requestedCounts)
				: generateFast(ctx, requestedCounts);
		addMissingDeclarations(ctx);
		return dropped;
	}

	private Set<ConstructId> generateFast(GenerationContext ctx, Map<ConstructId, Integer> requestedCounts) {
		Set<OWLAxiom> allAxioms = new HashSet<>();
		for (ConstructId id : orderPlanner.order(requestedCounts, ctx.random())) {
			checkElEligibility(ctx, id);
			allAxioms.addAll(registry.get(id).generate(ctx, requestedCounts.get(id)));
		}
		addAxioms(ctx, allAxioms);
		return Set.of();
	}

	private Set<ConstructId> generateWithConsistencyChecking(UUID jobId, GenerationContext ctx,
			Map<ConstructId, Integer> requestedCounts) {
		Set<ConstructId> dropped = new LinkedHashSet<>();
		for (ConstructId id : orderPlanner.order(requestedCounts, ctx.random())) {
			checkElEligibility(ctx, id);
			int count = requestedCounts.get(id);

			boolean accepted = false;
			for (int attempt = 0; attempt < maxBatchRetryAttempts; attempt++) {
				List<OWLAxiom> batch = registry.get(id).generate(ctx, count);
				addAxioms(ctx, batch);
				BatchCheckOutcome outcome = verificationService.checkConsistencyWithFallback(ctx.ontology(), intermediateCheckTimeout);
				boolean isFinalAttempt = attempt == maxBatchRetryAttempts - 1;
				diagnosticService.recordIfNotable(jobId, id, attempt, !outcome.keepBatch() && isFinalAttempt, outcome, ctx.ontology());
				if (outcome.keepBatch()) {
					accepted = true;
					break;
				}
				removeAxioms(ctx, batch);
			}
			if (!accepted) {
				dropped.add(id);
			}
		}
		return dropped;
	}

	private void checkElEligibility(GenerationContext ctx, ConstructId id) {
		// Defense in depth: RequestValidator already rejects EL-ineligible constructs at request time, so
		// this should never trigger — but if it ever does, fail loudly rather than silently generate (or
		// silently drop) axioms that would break the profile guarantee this request promised.
		if (ctx.targetProfile() == TargetProfile.EL && !id.elEligible()) {
			throw new IllegalStateException(
					id + " is not EL-eligible but reached generation under an EL target profile "
							+ "— this should have been rejected by RequestValidator");
		}
	}

	private void addAxioms(GenerationContext ctx, Collection<OWLAxiom> axioms) {
		axioms.forEach(axiom -> ctx.ontologyManager().addAxiom(ctx.ontology(), axiom));
	}

	private void removeAxioms(GenerationContext ctx, Collection<OWLAxiom> axioms) {
		axioms.forEach(axiom -> ctx.ontologyManager().removeAxiom(ctx.ontology(), axiom));
	}

	/**
	 * Pool entities can get minted mid-generation (e.g. an attachment strategy expanding the class pool
	 * because every existing class was already at its reuse cap) without a generator ever emitting a
	 * declaration axiom for them. Left undeclared, such an entity round-trips inconsistently: OWL API's
	 * RDF/XML parser synthesizes an implicit declaration for it on reload, silently inflating the axiom
	 * count relative to what was actually generated. Sweeping the ontology's full signature here guarantees
	 * every generated ontology is declaration-complete regardless of which generator path created an entity
	 * or which of the two generation modes above produced it.
	 */
	private void addMissingDeclarations(GenerationContext ctx) {
		Set<OWLEntity> declared = new HashSet<>();
		for (OWLAxiom axiom : ctx.ontology().getAxioms()) {
			if (axiom instanceof OWLDeclarationAxiom declarationAxiom) {
				declared.add(declarationAxiom.getEntity());
			}
		}
		for (OWLEntity entity : ctx.ontology().getSignature()) {
			if (!declared.contains(entity)) {
				ctx.ontologyManager().addAxiom(ctx.ontology(), ctx.dataFactory().getOWLDeclarationAxiom(entity));
			}
		}
	}
}
