package com.owl2gendl.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructCatalog;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.io.OntologyWriter;
import com.owl2gendl.orchestration.EntityProvisioningService;
import com.owl2gendl.orchestration.OntologyGenerationService;

/**
 * Generates every catalog construct in isolation (count=5 each) and checks it produces at least one axiom
 * and round-trips through OWL API's RDF/XML writer/reader with a stable axiom count. This is the M3
 * completeness guarantee: every construct the catalog advertises actually works, not just compiles.
 */
@SpringBootTest
class AllConstructsCoverageTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private OntologyGenerationService generationService;
	@Autowired
	private OntologyWriter ontologyWriter;
	@Autowired
	private ConstructCatalog catalog;

	@Test
	void catalogHasExactlyTheExpectedNumberOfConstructs() {
		// 67 minus the 3 redundant declaration constructs (Class/Object Property/Data Property Declaration)
		// removed because they silently minted extra entities beyond the Entity Pool counts, duplicating what
		// those top-level counts already control - see EntityProvisioningService.
		assertEquals(64, catalog.all().size());
	}

	@ParameterizedTest
	@EnumSource(ConstructId.class)
	void everyConstructGeneratesAndRoundTrips(ConstructId constructId) throws OWLOntologyCreationException, IOException {
		GenerationContext ctx = contextFactory.create(7L);
		// Generous seeding so every construct (including ones needing individuals/multiple distinct
		// entities, e.g. AllDisjointClasses, HasKey, NegativeObjectPropertyAssertion) has enough to work with.
		provisioningService.seed(ctx, new EntityCountsDto(8, 5, 5, 8));

		Map<ConstructId, Integer> requested = new EnumMap<>(ConstructId.class);
		requested.put(constructId, 5);
		generationService.generate(UUID.randomUUID(), ctx, requested, false);

		long generatedAxiomCount = ctx.ontology().getAxiomCount();
		assertTrue(generatedAxiomCount > 0, constructId + " produced no axioms");

		String rdfXml = ontologyWriter.write(ctx.ontology(), OntologyWriter.Format.RDFXML);
		OWLOntology reloaded = OWLManager.createOWLOntologyManager()
				.loadOntologyFromOntologyDocument(new ByteArrayInputStream(rdfXml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(generatedAxiomCount, reloaded.getAxiomCount(),
				constructId + " did not round-trip with a stable axiom count");
	}
}
