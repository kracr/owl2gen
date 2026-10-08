package com.owl2gendl.metrics;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import openllet.owlapi.OpenlletReasonerFactory;

/**
 * Counts entailed subclass relationships (the full ancestor closure, not just directly-asserted edges) that
 * were never explicitly stated — e.g. given asserted {@code A⊑B} and {@code B⊑C}, the reasoner also entails
 * {@code A⊑C} even though no axiom says so directly. This is the signal that the ontology supports
 * non-trivial inference rather than only restating what's already obvious; using {@code direct=true} here
 * would just re-derive the asserted edges themselves and never surface anything new.
 *
 * <p>Boots its own short-lived Openllet reasoner rather than reusing {@code VerificationService}'s (which
 * only exposes a pass/fail result, not the reasoner instance) — a real but acceptable duplication of
 * reasoner setup cost for now; consolidating them is a reasonable future optimization if that cost matters.
 *
 * <p>Timeout-bounded and tolerant of reasoner failure, for two reasons confirmed the hard way while running
 * real experiments: an inconsistent ontology makes classification meaningless and Openllet throws rather
 * than returning "everything entails everything" (an uncaught exception here previously destroyed an
 * already-computed CONSISTENT/INCONSISTENT verdict for the whole variant); and full classification
 * ({@code precomputeInferences(CLASS_HIERARCHY)}) can be significantly more expensive than the plain
 * consistency check {@code VerificationService} bounds — for a TBox with transitive properties, property
 * chains, and qualified cardinalities, this step alone was observed to run for at least ten minutes with no
 * bound at all before this fix, silently breaking the "reasoning never hangs the request" guarantee the rest
 * of the reasoning layer was built around.
 */
@Component
public class InferredVsAssertedCalculator {

	private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
		Thread thread = new Thread(runnable, "owl2gendl-metrics-reasoner");
		thread.setDaemon(true);
		return thread;
	});

	public ReasoningMetrics analyze(OWLOntology ontology, Duration timeout) {
		Set<OWLSubClassOfAxiom> asserted = ontology.getAxioms(AxiomType.SUBCLASS_OF);
		OWLReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

		Callable<ReasoningMetrics> task = () -> classify(ontology, asserted, reasoner);
		Future<ReasoningMetrics> future = executor.submit(task);
		try {
			return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			reasoner.interrupt();
			future.cancel(true);
			return ReasoningMetrics.notComputed(asserted.size());
		} finally {
			reasoner.dispose();
		}
	}

	private ReasoningMetrics classify(OWLOntology ontology, Set<OWLSubClassOfAxiom> asserted, OWLReasoner reasoner) {
		OWLDataFactory factory = ontology.getOWLOntologyManager().getOWLDataFactory();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		int inferredNew = 0;
		for (OWLClass owlClass : ontology.getClassesInSignature()) {
			for (OWLClass superClass : reasoner.getSuperClasses(owlClass, false).getFlattened()) {
				if (superClass.isOWLThing() || superClass.equals(owlClass)) {
					continue;
				}
				if (!asserted.contains(factory.getOWLSubClassOfAxiom(owlClass, superClass))) {
					inferredNew++;
				}
			}
		}
		double ratio = asserted.isEmpty() ? 0.0 : (double) inferredNew / asserted.size();
		return new ReasoningMetrics(true, asserted.size(), inferredNew, ratio);
	}

	@PreDestroy
	public void shutdown() {
		executor.shutdownNow();
	}
}
