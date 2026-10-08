package com.owl2gendl.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.api.dto.GenerationJobDto;
import com.owl2gendl.api.dto.GenerationRequestDto;
import com.owl2gendl.api.dto.OntologyGraphDto;
import com.owl2gendl.api.dto.VariantResultDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.graph.OntologyGraphBuilder;
import com.owl2gendl.io.OntologyWriter;
import com.owl2gendl.job.GenerationJob;
import com.owl2gendl.job.GenerationJobRunner;
import com.owl2gendl.job.GenerationJobStore;
import com.owl2gendl.job.VariantOutcome;
import com.owl2gendl.orchestration.EntityCountsResolver;
import com.owl2gendl.topology.TopologyConfig;
import com.owl2gendl.topology.TopologyVariant;

import jakarta.validation.Valid;

/**
 * Async, potentially-multi-variant generation API. Superseeds {@code /api/generate} (kept as a fast,
 * synchronous, single-variant dev shortcut from M2) for real usage — reasoning is timeout-bounded but can
 * still take real time, and a request may ask for more than one topology variant, so this is a
 * submit-then-poll job API rather than a single blocking call.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:5173"})
public class GenerationJobController {

	private final GenerationJobStore jobStore;
	private final GenerationJobRunner jobRunner;
	private final OntologyWriter ontologyWriter;
	private final RequestValidator requestValidator;
	private final EntityCountsResolver entityCountsResolver;
	private final OntologyGraphBuilder ontologyGraphBuilder;

	public GenerationJobController(GenerationJobStore jobStore, GenerationJobRunner jobRunner, OntologyWriter ontologyWriter,
			RequestValidator requestValidator, EntityCountsResolver entityCountsResolver, OntologyGraphBuilder ontologyGraphBuilder) {
		this.jobStore = jobStore;
		this.jobRunner = jobRunner;
		this.ontologyWriter = ontologyWriter;
		this.requestValidator = requestValidator;
		this.entityCountsResolver = entityCountsResolver;
		this.ontologyGraphBuilder = ontologyGraphBuilder;
	}

	@PostMapping("/api/generations")
	public ResponseEntity<GenerationJobDto> submit(@Valid @RequestBody GenerationRequestDto request) {
		requestValidator.validate(request);
		Set<ConstructId> selected = request.constructs().keySet().stream().map(ConstructId::valueOf).collect(Collectors.toSet());
		EntityCountsDto resolvedCounts = entityCountsResolver.resolve(request.entityCounts(), selected);
		GenerationRequestDto resolvedRequest = new GenerationRequestDto(resolvedCounts, request.constructs(), request.seed(),
				request.structure(), request.variants(), request.reasoning(), request.targetProfile(), request.strictConsistency());
		List<TopologyVariant> variants = resolveVariants(resolvedRequest.variants());
		GenerationJob job = jobStore.create(resolvedRequest, variants);
		jobRunner.run(job);
		return ResponseEntity.accepted().body(toDto(job));
	}

	@GetMapping("/api/generations/{jobId}")
	public ResponseEntity<GenerationJobDto> get(@PathVariable String jobId) {
		return jobStore.find(UUID.fromString(jobId))
				.map(job -> ResponseEntity.ok(toDto(job)))
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/api/generations/{jobId}/variants/{variantId}/ontology")
	public ResponseEntity<String> downloadOntology(@PathVariable String jobId, @PathVariable String variantId,
			@RequestParam(defaultValue = "RDFXML") String format) {
		VariantOutcome outcome = findOutcomeOrThrow(jobId, variantId);
		if (outcome.ontology() == null) {
			return ResponseEntity.notFound().build();
		}
		OntologyWriter.Format parsedFormat = OntologyWriter.parse(format);
		String serialized = ontologyWriter.write(outcome.ontology(), parsedFormat);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(ontologyWriter.contentType(parsedFormat)))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						"attachment; filename=\"" + variantId.toLowerCase() + "." + ontologyWriter.fileExtension(parsedFormat) + "\"")
				.body(serialized);
	}

	@GetMapping("/api/generations/{jobId}/variants/{variantId}/graph")
	public ResponseEntity<OntologyGraphDto> variantGraph(@PathVariable String jobId, @PathVariable String variantId) {
		VariantOutcome outcome = findOutcomeOrThrow(jobId, variantId);
		if (outcome.ontology() == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(ontologyGraphBuilder.build(outcome.ontology()));
	}

	@GetMapping("/api/generations/{jobId}/variants/{variantId}/metrics")
	public ResponseEntity<VariantResultDto> variantResult(@PathVariable String jobId, @PathVariable String variantId) {
		GenerationJob job = jobStore.find(UUID.fromString(jobId)).orElse(null);
		if (job == null) {
			return ResponseEntity.notFound().build();
		}
		TopologyVariant variant = TopologyVariant.valueOf(variantId.toUpperCase());
		VariantOutcome outcome = job.outcome(variant);
		if (outcome == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(toVariantDto(variant, outcome));
	}

	private VariantOutcome findOutcomeOrThrow(String jobId, String variantId) {
		GenerationJob job = jobStore.find(UUID.fromString(jobId))
				.orElseThrow(() -> new IllegalArgumentException("No such job: " + jobId));
		TopologyVariant variant = TopologyVariant.valueOf(variantId.toUpperCase());
		VariantOutcome outcome = job.outcome(variant);
		if (outcome == null) {
			throw new IllegalArgumentException("Job " + jobId + " did not request variant " + variantId);
		}
		return outcome;
	}

	private List<TopologyVariant> resolveVariants(List<String> requested) {
		if (requested == null || requested.isEmpty()) {
			return List.of(TopologyConfig.defaults().variant());
		}
		return requested.stream().map(name -> TopologyVariant.valueOf(name.toUpperCase())).toList();
	}

	private GenerationJobDto toDto(GenerationJob job) {
		List<VariantResultDto> variants = job.requestedVariants().stream()
				.map(variant -> toVariantDto(variant, job.outcome(variant)))
				.toList();
		return new GenerationJobDto(job.id().toString(), job.status().name(), job.createdAt().toString(), variants, job.errorMessage());
	}

	private VariantResultDto toVariantDto(TopologyVariant variant, VariantOutcome outcome) {
		Long axiomCount = outcome.ontology() != null ? (long) outcome.ontology().getAxiomCount() : null;
		return new VariantResultDto(variant.name(), outcome.status().name(), axiomCount, outcome.errorMessage(),
				outcome.verification(), outcome.metrics());
	}
}
