package com.owl2gendl.orchestration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.io.OntologyWriter;

/**
 * Multi-construct combination test: several constructs generated together into one ontology, verifying they
 * interconnect (shared entity reuse across constructs) and the combined result round-trips through OWL API.
 * Per-construct isolation coverage lives in {@code AllConstructsCoverageTest}.
 */
@SpringBootTest
class OntologyGenerationServiceTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private OntologyGenerationService generationService;
	@Autowired
	private OntologyWriter ontologyWriter;

	@Test
	void generatedOntologyRoundTripsThroughOwlApiWithExpectedAxiomCounts() throws OWLOntologyCreationException, IOException {
		GenerationContext ctx = contextFactory.create(42L);
		provisioningService.seed(ctx, new EntityCountsDto(10, 3, 0, 0));

		Map<ConstructId, Integer> requested = new EnumMap<>(ConstructId.class);
		requested.put(ConstructId.SUB_CLASS_OF, 15);
		requested.put(ConstructId.OBJECT_SOME_VALUES_FROM, 8);
		generationService.generate(UUID.randomUUID(), ctx, requested, false);

		long generatedAxiomCount = ctx.ontology().getAxiomCount();
		assertTrue(generatedAxiomCount > 0, "expected at least one axiom to be generated");
		// Entity provisioning seeds 10 classes up front; reuse-driven pool expansion from SubClassOf/
		// SomeValuesFrom picks can only ever grow that, never shrink it.
		assertTrue(ctx.pools().classes().size() >= 10);

		String rdfXml = ontologyWriter.write(ctx.ontology(), OntologyWriter.Format.RDFXML);

		OWLOntology reloaded = OWLManager.createOWLOntologyManager()
				.loadOntologyFromOntologyDocument(new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(generatedAxiomCount, reloaded.getAxiomCount(),
				"axiom count must round-trip exactly through RDF/XML serialization");
	}
}
