package com.owl2gendl.job;

import org.semanticweb.owlapi.model.OWLOntology;

import com.owl2gendl.metrics.MetricsReport;
import com.owl2gendl.reasoning.VerificationResult;
import com.owl2gendl.topology.TopologyVariant;

/** Mutable per-variant run state, updated in place by {@code GenerationJobRunner} as work completes. */
public class VariantOutcome {

	private final TopologyVariant variant;
	private volatile RunStatus status = RunStatus.PENDING;
	private volatile OWLOntology ontology;
	private volatile VerificationResult verification;
	private volatile MetricsReport metrics;
	private volatile String errorMessage;

	public VariantOutcome(TopologyVariant variant) {
		this.variant = variant;
	}

	public TopologyVariant variant() {
		return variant;
	}

	public RunStatus status() {
		return status;
	}

	public void markRunning() {
		this.status = RunStatus.RUNNING;
	}

	public void markCompleted(OWLOntology ontology, VerificationResult verification, MetricsReport metrics) {
		this.ontology = ontology;
		this.verification = verification;
		this.metrics = metrics;
		this.status = RunStatus.COMPLETED;
	}

	public void markFailed(String errorMessage) {
		this.errorMessage = errorMessage;
		this.status = RunStatus.FAILED;
	}

	public OWLOntology ontology() {
		return ontology;
	}

	public VerificationResult verification() {
		return verification;
	}

	public MetricsReport metrics() {
		return metrics;
	}

	public String errorMessage() {
		return errorMessage;
	}
}
