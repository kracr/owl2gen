package com.owl2gendl.reasoning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.owl2gendl.io.OntologyWriter;

class VerificationServiceTest {

	private final VerificationService service = new VerificationService(new OwlProfileChecker(), new ReasonerTierSelector(),
			new KoncludeConsistencyChecker(new OntologyWriter(), "", "AUTO"), Optional.empty(), List.of("OPENLLET"));

	@Test
	void elCompliantOntologyIsRecognizedAsSuchAndVerifiesConsistentViaOpenllet() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology(IRI.create("http://owl2gendl.com/test#el"));
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create("http://owl2gendl.com/test#A"));
		OWLClass b = factory.getOWLClass(IRI.create("http://owl2gendl.com/test#B"));
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create("http://owl2gendl.com/test#p"));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(a));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(b));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(p));
		// SubClassOf and ObjectSomeValuesFrom are both EL-legal - profile metadata should reflect that
		// even though only Openllet actually runs (ELK was dropped - see ReasonerTier's javadoc).
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(a, factory.getOWLObjectSomeValuesFrom(p, b)));

		VerificationResult result = service.verify(ontology, null, Duration.ofSeconds(30));

		assertTrue(result.profile().el(), "expected this ontology to be recognized as EL-profile compliant");
		assertEquals(ReasonerTier.OPENLLET, result.tier());
		assertEquals(VerificationStatus.CONSISTENT, result.status());
	}

	@Test
	void nonElOntologyIsRecognizedAsSuchAndStillVerifiesViaOpenllet() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology(IRI.create("http://owl2gendl.com/test#dl"));
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create("http://owl2gendl.com/test#A"));
		OWLClass b = factory.getOWLClass(IRI.create("http://owl2gendl.com/test#B"));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(a));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(b));
		// ObjectUnionOf is not permitted on the left-hand side of SubClassOf in EL.
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(factory.getOWLObjectUnionOf(a, b), a));

		VerificationResult result = service.verify(ontology, null, Duration.ofSeconds(30));

		assertFalse(result.profile().el(), "expected this ontology to be recognized as not EL-profile compliant");
		assertEquals(ReasonerTier.OPENLLET, result.tier());
		assertEquals(VerificationStatus.CONSISTENT, result.status());
	}

	@Test
	void explicitOverrideIsHonored() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology(IRI.create("http://owl2gendl.com/test#override"));
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create("http://owl2gendl.com/test#A"));
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(a));

		VerificationResult result = service.verify(ontology, ReasonerTier.OPENLLET, Duration.ofSeconds(30));

		assertEquals(ReasonerTier.OPENLLET, result.tier());
	}

	@Test
	void artificiallyTinyTimeoutReportsTimeoutRatherThanHangingOrThrowing() throws OWLOntologyCreationException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = manager.createOntology(IRI.create("http://owl2gendl.com/test#timeout"));
		OWLDataFactory factory = manager.getOWLDataFactory();
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(factory.getOWLClass(IRI.create("http://owl2gendl.com/test#A"))));

		VerificationResult result = service.verify(ontology, ReasonerTier.OPENLLET, Duration.ofNanos(1));

		assertEquals(VerificationStatus.NOT_VERIFIED_TIMEOUT, result.status());
	}
}
