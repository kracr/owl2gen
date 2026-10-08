package com.owl2gendl.api;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.api.dto.GenerationRequestDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.io.OntologyWriter;
import com.owl2gendl.orchestration.EntityCountsResolver;
import com.owl2gendl.orchestration.EntityProvisioningService;
import com.owl2gendl.orchestration.OntologyGenerationService;

import jakarta.validation.Valid;

/**
 * Walking-skeleton generation endpoint (M2): synchronous, single-variant, no topology/constraint/reasoning
 * layers yet. Superseded by the async multi-variant job API in M8 — kept afterward as a fast dev-only path.
 */
@RestController
@CrossOrigin(origins = {"http://localhost:5173"})
public class GenerationController {

	private final GenerationContextFactory contextFactory;
	private final EntityProvisioningService provisioningService;
	private final OntologyGenerationService generationService;
	private final OntologyWriter ontologyWriter;
	private final RequestValidator requestValidator;
	private final EntityCountsResolver entityCountsResolver;

	public GenerationController(GenerationContextFactory contextFactory, EntityProvisioningService provisioningService,
			OntologyGenerationService generationService, OntologyWriter ontologyWriter, RequestValidator requestValidator,
			EntityCountsResolver entityCountsResolver) {
		this.contextFactory = contextFactory;
		this.provisioningService = provisioningService;
		this.generationService = generationService;
		this.ontologyWriter = ontologyWriter;
		this.requestValidator = requestValidator;
		this.entityCountsResolver = entityCountsResolver;
	}

	@PostMapping("/api/generate")
	public ResponseEntity<String> generate(@Valid @RequestBody GenerationRequestDto request,
			@RequestParam(defaultValue = "RDFXML") String format) {
		requestValidator.validate(request);
		GenerationContext ctx = contextFactory.create(request.seed());
		Map<ConstructId, Integer> requestedCounts = new EnumMap<>(ConstructId.class);
		request.constructs().forEach((name, count) -> requestedCounts.put(ConstructId.valueOf(name), count));
		Set<ConstructId> selected = requestedCounts.keySet();
		EntityCountsDto resolvedCounts = entityCountsResolver.resolve(request.entityCounts(), selected);
		provisioningService.seed(ctx, resolvedCounts);

		// No real job/UUID exists on this dev-only synchronous path; a fresh id just scopes this one call's
		// diagnostics directory, same as any other job would.
		generationService.generate(UUID.randomUUID(), ctx, requestedCounts, Boolean.TRUE.equals(request.strictConsistency()));

		OntologyWriter.Format parsedFormat = OntologyWriter.parse(format);
		String serialized = ontologyWriter.write(ctx.ontology(), parsedFormat);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(ontologyWriter.contentType(parsedFormat)))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ontology." + ontologyWriter.fileExtension(parsedFormat) + "\"")
				.header("X-Axiom-Count", String.valueOf(ctx.ontology().getAxiomCount()))
				.header("X-Class-Count", String.valueOf(ctx.pools().classes().size()))
				.body(serialized);
	}
}
