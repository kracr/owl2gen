package com.owl2gendl.job;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.owl2gendl.api.dto.GenerationRequestDto;
import com.owl2gendl.topology.TopologyVariant;

public class GenerationJob {

	private final UUID id;
	private final GenerationRequestDto request;
	private final List<TopologyVariant> requestedVariants;
	private final Instant createdAt = Instant.now();
	private final Map<TopologyVariant, VariantOutcome> outcomes = new EnumMap<>(TopologyVariant.class);
	private volatile RunStatus status = RunStatus.PENDING;
	private volatile String errorMessage;

	public GenerationJob(UUID id, GenerationRequestDto request, List<TopologyVariant> requestedVariants) {
		this.id = id;
		this.request = request;
		this.requestedVariants = requestedVariants;
		for (TopologyVariant variant : requestedVariants) {
			outcomes.put(variant, new VariantOutcome(variant));
		}
	}

	public UUID id() {
		return id;
	}

	public GenerationRequestDto request() {
		return request;
	}

	public List<TopologyVariant> requestedVariants() {
		return requestedVariants;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public RunStatus status() {
		return status;
	}

	public void markRunning() {
		this.status = RunStatus.RUNNING;
	}

	public void markCompleted() {
		this.status = RunStatus.COMPLETED;
	}

	public void markFailed(String errorMessage) {
		this.errorMessage = errorMessage;
		this.status = RunStatus.FAILED;
	}

	public String errorMessage() {
		return errorMessage;
	}

	public VariantOutcome outcome(TopologyVariant variant) {
		return outcomes.get(variant);
	}

	public Map<TopologyVariant, VariantOutcome> outcomes() {
		return outcomes;
	}
}
