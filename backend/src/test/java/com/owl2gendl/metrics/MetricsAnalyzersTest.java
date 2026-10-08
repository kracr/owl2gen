package com.owl2gendl.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/** Hand-built small ontologies with known expected metric values — verifies exact numbers, not just "runs". */
class MetricsAnalyzersTest {

	private static final String NS = "http://owl2gendl.com/test#";

	private OWLOntology newOntology(String name) throws OWLOntologyCreationException {
		return OWLManager.createOWLOntologyManager().createOntology(IRI.create(NS + name));
	}

	@Test
	void nestingDepthAnalyzerComputesExactDepthForAKnownExpression() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("nesting");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create(NS + "A"));
		OWLClass b = factory.getOWLClass(IRI.create(NS + "B"));
		OWLClass c = factory.getOWLClass(IRI.create(NS + "C"));
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create(NS + "p"));

		// SubClassOf(A, IntersectionOf(SomeValuesFrom(p, B), C)) -> nested expressions: {A, Intersection,
		// SomeValuesFrom, B, C} with depths {0, 2, 1, 0, 0}: max=2, avg=3/5=0.6
		OWLClassExpression someValuesFrom = factory.getOWLObjectSomeValuesFrom(p, b);
		OWLClassExpression intersection = factory.getOWLObjectIntersectionOf(someValuesFrom, c);
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(a, intersection));

		NestingMetrics result = new NestingDepthAnalyzer().analyze(ontology);

		assertEquals(2, result.maxDepth());
		assertEquals(0.6, result.avgDepth(), 1e-9);
	}

	@Test
	void hierarchyAnalyzerComputesExactDepthBranchingAndTangledness() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("hierarchy");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create(NS + "A"));
		OWLClass b = factory.getOWLClass(IRI.create(NS + "B"));
		OWLClass c = factory.getOWLClass(IRI.create(NS + "C"));
		OWLClass d = factory.getOWLClass(IRI.create(NS + "D"));

		// A -> B -> C (depth 2, 1, 0) and D -> C (depth 1). C has 2 children (B, D): avg branching (2+1)/2=1.5.
		// No class has more than one direct parent: tangledness 0.
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(a, b));
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(b, c));
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(d, c));

		HierarchyMetrics result = new HierarchyAnalyzer().analyze(ontology);

		assertEquals(2, result.maxDepth());
		assertEquals(1.5, result.avgBranchingFactor(), 1e-9);
		assertEquals(0.0, result.tangledness(), 1e-9);
	}

	@Test
	void hierarchyAnalyzerDetectsTangledness() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("tangled");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create(NS + "A"));
		OWLClass b = factory.getOWLClass(IRI.create(NS + "B"));
		OWLClass c = factory.getOWLClass(IRI.create(NS + "C"));

		// A has two direct parents (B and C): the only tracked class (A) is tangled -> tangledness 1.0.
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(a, b));
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(a, c));

		HierarchyMetrics result = new HierarchyAnalyzer().analyze(ontology);

		assertEquals(1.0, result.tangledness(), 1e-9);
	}

	@Test
	void graphMetricsCalculatorComputesExactNodeEdgeAndDegreeCounts() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("graph");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create(NS + "p"));
		OWLNamedIndividual ind1 = factory.getOWLNamedIndividual(IRI.create(NS + "ind1"));
		OWLNamedIndividual ind2 = factory.getOWLNamedIndividual(IRI.create(NS + "ind2"));
		OWLNamedIndividual ind3 = factory.getOWLNamedIndividual(IRI.create(NS + "ind3"));

		// Two assertions, each with signature {p, subject, object} -> pairwise edges per axiom:
		// (p,ind1),(p,ind2),(ind1,ind2) and (p,ind2)[dup],(p,ind3),(ind2,ind3)
		// Vertices: {p, ind1, ind2, ind3} = 4. Unique edges: 5. avgDegree = 2*5/4 = 2.5.
		manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(p, ind1, ind2));
		manager.addAxiom(ontology, factory.getOWLObjectPropertyAssertionAxiom(p, ind2, ind3));

		Map<OWLEntity, Set<OWLEntity>> adjacency = new EntityRelationGraphBuilder().build(ontology);
		GraphMetrics result = new GraphMetricsCalculator().analyze(adjacency);

		assertEquals(4, result.nodeCount());
		assertEquals(5, result.edgeCount());
		assertEquals(2.5, result.avgDegree(), 1e-9);
	}

	@Test
	void inferredVsAssertedCalculatorCountsTransitiveInferenceNotDirectlyAsserted() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("inference");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create(NS + "A"));
		OWLClass b = factory.getOWLClass(IRI.create(NS + "B"));
		OWLClass c = factory.getOWLClass(IRI.create(NS + "C"));

		// Asserted: A subClassOf B, B subClassOf C (2 axioms). Entailed but never stated: A subClassOf C.
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(a, b));
		manager.addAxiom(ontology, factory.getOWLSubClassOfAxiom(b, c));

		ReasoningMetrics result = new InferredVsAssertedCalculator().analyze(ontology, Duration.ofSeconds(30));

		assertTrue(result.computed());
		assertEquals(2, result.assertedSubClassOfCount());
		assertEquals(1, result.inferredNewSubClassOfCount());
		assertEquals(0.5, result.inferredToAssertedRatio(), 1e-9);
	}

	@Test
	void inferredVsAssertedCalculatorDegradesGracefullyOnAnInconsistentOntology() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("inconsistent");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		OWLClass a = factory.getOWLClass(IRI.create(NS + "A"));
		OWLClassExpression notA = factory.getOWLObjectComplementOf(a);
		var individual = factory.getOWLNamedIndividual(IRI.create(NS + "ind1"));

		// An individual asserted to be both A and not-A: unsatisfiable, so classification is undefined.
		manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(a, individual));
		manager.addAxiom(ontology, factory.getOWLClassAssertionAxiom(notA, individual));

		ReasoningMetrics result = new InferredVsAssertedCalculator().analyze(ontology, Duration.ofSeconds(30));

		assertFalse(result.computed());
	}

	@Test
	void inferredVsAssertedCalculatorRespectsAnArtificiallyTinyTimeout() throws OWLOntologyCreationException {
		OWLOntology ontology = newOntology("timeout");
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory factory = manager.getOWLDataFactory();
		manager.addAxiom(ontology, factory.getOWLDeclarationAxiom(factory.getOWLClass(IRI.create(NS + "A"))));

		// Regression test: this step used to have no time bound at all and was observed to run unbounded
		// for at least ten minutes on a sufficiently hard ontology (see class javadoc). A near-zero budget
		// must reliably report "not computed" rather than hang, exactly like VerificationService does.
		ReasoningMetrics result = new InferredVsAssertedCalculator().analyze(ontology, Duration.ofNanos(1));

		assertFalse(result.computed());
	}
}
