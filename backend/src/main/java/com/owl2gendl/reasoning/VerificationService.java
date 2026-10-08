package com.owl2gendl.reasoning;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Runs consistency checking under a hard time budget and never lets a slow/hanging reasoner block the
 * caller — full DL consistency checking is worst-case N2ExpTime, so for large/expressive requests this
 * degrading gracefully (reporting NOT_VERIFIED_TIMEOUT as data) is the honest alternative to pretending
 * every request can be verified in bounded time.
 *
 * <p>Note: interrupting the reasoner thread on timeout is best-effort for the in-JVM tiers (OPENLLET,
 * HERMIT, CUSTOM) — OWL API reasoners are not guaranteed to check their interrupted status during internal
 * computation, so a timed-out reasoner may keep consuming CPU in the background after this returns. KONCLUDE
 * does not have this limitation: it runs as a real OS process, which {@link KoncludeConsistencyChecker} kills
 * outright on timeout.
 */
@Service
public class VerificationService {

	private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

	private final OwlProfileChecker profileChecker;
	private final ReasonerTierSelector tierSelector;
	private final KoncludeConsistencyChecker koncludeChecker;
	private final Optional<CustomReasonerFactory> customReasonerFactory;
	private final List<ReasonerTier> fallbackOrder;
	private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
		Thread thread = new Thread(runnable, "owl2gendl-reasoner");
		thread.setDaemon(true);
		return thread;
	});

	public VerificationService(OwlProfileChecker profileChecker, ReasonerTierSelector tierSelector,
			KoncludeConsistencyChecker koncludeChecker, Optional<CustomReasonerFactory> customReasonerFactory,
			@Value("${owl2gendl.reasoning.fallback-order:OPENLLET}") List<String> fallbackOrder) {
		this.profileChecker = profileChecker;
		this.tierSelector = tierSelector;
		this.koncludeChecker = koncludeChecker;
		this.customReasonerFactory = customReasonerFactory;
		this.fallbackOrder = fallbackOrder.stream().map(String::trim).map(ReasonerTier::valueOf).toList();
	}

	public VerificationResult verify(OWLOntology ontology, ReasonerTier override, Duration timeout) {
		return verify(ontology, override, timeout, TargetProfile.DL);
	}

	public VerificationResult verify(OWLOntology ontology, ReasonerTier override, Duration timeout,
			TargetProfile requestedProfile) {
		ProfileCheckResult profile = profileChecker.check(ontology);
		// DL is checked the same way EL is, not left null: local generation-time guards (ConstraintTracker)
		// are the efficient preventive mechanism, but a guard omission should never silently become part of
		// the reported result - this OWL2DLProfile check is the safety net that catches it regardless of
		// which guard (if any) missed it. Real case: a gap in property-simplicity tracking (fixed, but stood
		// as a reminder that this check earns its cost) let a non-simple property slip into
		// DisjointObjectProperties undetected by generation-time tracking alone.
		Boolean profileGuaranteeSatisfied = switch (requestedProfile) {
			case DL -> profile.dl();
			case EL -> profile.el();
		};
		ReasonerTier tier = tierSelector.select(override);

		long start = System.nanoTime();
		VerificationStatus status = checkOnce(ontology, tier, timeout);
		return new VerificationResult(status, tier, profile, elapsedMillis(start), requestedProfile, profileGuaranteeSatisfied);
	}

	/**
	 * Consistency-only, no profile check — used for the intermediate checks during {@code strictConsistency}
	 * generation, where dozens of calls may happen per variant and the OWL2 profile check {@link #verify}
	 * always does would be pure waste until the one final, authoritative call.
	 *
	 * <p>Tries every tier in {@code owl2gendl.reasoning.fallback-order} in turn, stopping at the first
	 * definite (CONSISTENT/INCONSISTENT) answer — a batch that one reasoner cannot verify within budget may
	 * still be resolved quickly by another with different internal algorithms/optimizations, which is exactly
	 * the scenario this project wants to surface for its own reasoner-comparison evaluation, not just paper
	 * over. Returns every tier actually attempted and its outcome, for {@code BatchDiagnosticService} to log.
	 *
	 * <p>Fails open when no tier in the fallback order reaches a definite answer within budget: this is a
	 * best-effort safety net layered under generation, not the authoritative check (that's still
	 * {@link #verify}, with its own, separately-configured budget) — treat the batch as acceptable rather than
	 * block generation on it.
	 */
	public BatchCheckOutcome checkConsistencyWithFallback(OWLOntology ontology, Duration perTierTimeout) {
		Map<ReasonerTier, VerificationStatus> attempts = new LinkedHashMap<>();
		for (ReasonerTier tier : fallbackOrder) {
			long start = System.nanoTime();
			VerificationStatus status = checkOnce(ontology, tier, perTierTimeout);
			attempts.put(tier, status);
			if (status == VerificationStatus.CONSISTENT || status == VerificationStatus.INCONSISTENT) {
				return new BatchCheckOutcome(true, status == VerificationStatus.CONSISTENT, attempts);
			}
			log.debug("Reasoner tier {} did not reach a definite answer in {} ms ({})", tier,
					elapsedMillis(start), status);
		}
		// No tier resolved it within budget: fail open (treat as acceptable), matching this method's
		// documented safety-net role — but resolved=false records that this was a guess, not a verdict.
		return new BatchCheckOutcome(false, true, attempts);
	}

	/** Backward-compatible single-tier form, kept for call sites that do not need fallback or diagnostics. */
	public boolean checkConsistency(OWLOntology ontology, Duration timeout) {
		VerificationStatus status = checkOnce(ontology, ReasonerTier.OPENLLET, timeout);
		return status != VerificationStatus.INCONSISTENT;
	}

	private VerificationStatus checkOnce(OWLOntology ontology, ReasonerTier tier, Duration timeout) {
		if (tier == ReasonerTier.KONCLUDE) {
			return koncludeChecker.check(ontology, timeout);
		}
		OWLReasoner reasoner;
		try {
			reasoner = createReasoner(tier, ontology);
		} catch (IllegalStateException e) {
			log.warn("Could not create reasoner for tier {}: {}", tier, e.getMessage());
			return VerificationStatus.NOT_VERIFIED_ERROR;
		}
		Future<Boolean> future = executor.submit(reasoner::isConsistent);
		try {
			boolean consistent = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
			return consistent ? VerificationStatus.CONSISTENT : VerificationStatus.INCONSISTENT;
		} catch (TimeoutException e) {
			reasoner.interrupt();
			future.cancel(true);
			return VerificationStatus.NOT_VERIFIED_TIMEOUT;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.warn("Verification interrupted", e);
			return VerificationStatus.NOT_VERIFIED_ERROR;
		} catch (ExecutionException e) {
			log.warn("Verification failed with a reasoner exception (tier {})", tier, e.getCause());
			return VerificationStatus.NOT_VERIFIED_ERROR;
		}
	}

	private long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	private OWLReasoner createReasoner(ReasonerTier tier, OWLOntology ontology) {
		return switch (tier) {
			case OPENLLET -> openllet.owlapi.OpenlletReasonerFactory.getInstance().createReasoner(ontology);
			case HERMIT -> new org.semanticweb.HermiT.ReasonerFactory().createReasoner(ontology);
			case CUSTOM -> customReasonerFactory
					.orElseThrow(() -> new IllegalStateException(
							"ReasonerTier.CUSTOM was selected but no CustomReasonerFactory bean is registered"))
					.createReasoner(ontology);
			case KONCLUDE -> throw new IllegalStateException("KONCLUDE has no OWLReasoner binding; use checkOnce");
		};
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdownNow();
	}
}
