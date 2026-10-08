package com.owl2gendl.constraint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.context.GenerationContextFactory;
import com.owl2gendl.generator.assertion.DifferentIndividualsGenerator;
import com.owl2gendl.generator.assertion.NegativeObjectPropertyAssertionGenerator;
import com.owl2gendl.generator.assertion.ObjectPropertyAssertionGenerator;
import com.owl2gendl.generator.assertion.SameIndividualGenerator;
import com.owl2gendl.generator.classaxiom.AllDisjointClassesGenerator;
import com.owl2gendl.generator.classaxiom.DisjointUnionGenerator;
import com.owl2gendl.generator.classaxiom.DisjointWithGenerator;
import com.owl2gendl.generator.classaxiom.EquivalentClassesGenerator;
import com.owl2gendl.generator.datapropaxiom.AllDisjointDataPropertiesGenerator;
import com.owl2gendl.generator.datapropaxiom.EquivalentDataPropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.AllDisjointObjectPropertiesGenerator;
import com.owl2gendl.generator.objectpropaxiom.AsymmetricPropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.EquivalentObjectPropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.InverseFunctionalPropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.IrreflexivePropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.ObjectPropertyDisjointWithGenerator;
import com.owl2gendl.generator.objectpropaxiom.ReflexivePropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.SymmetricPropertyGenerator;
import com.owl2gendl.generator.objectpropaxiom.TransitivePropertyGenerator;
import com.owl2gendl.generator.objectproprestriction.ObjectHasSelfGenerator;
import com.owl2gendl.orchestration.EntityProvisioningService;

/**
 * Reproduces the clash scenarios the old codebase's ad-hoc {@code discardConcepts}/{@code discardProperties}
 * bookkeeping was meant to prevent, and confirms {@link ConstraintTracker} blocks them by construction. Each
 * test seeds only as many entities as the clash needs (1 property, or 1 class pair), so a blocked axiom has
 * no alternative entity to fall back to - it must come back empty rather than silently produce a clash.
 */
@SpringBootTest
class ConstraintTrackerTest {

	@Autowired
	private GenerationContextFactory contextFactory;
	@Autowired
	private EntityProvisioningService provisioningService;
	@Autowired
	private AsymmetricPropertyGenerator asymmetricPropertyGenerator;
	@Autowired
	private SymmetricPropertyGenerator symmetricPropertyGenerator;
	@Autowired
	private ReflexivePropertyGenerator reflexivePropertyGenerator;
	@Autowired
	private IrreflexivePropertyGenerator irreflexivePropertyGenerator;
	@Autowired
	private DisjointWithGenerator disjointWithGenerator;
	@Autowired
	private EquivalentClassesGenerator equivalentClassesGenerator;
	@Autowired
	private ObjectPropertyDisjointWithGenerator objectPropertyDisjointWithGenerator;
	@Autowired
	private EquivalentObjectPropertyGenerator equivalentObjectPropertyGenerator;
	@Autowired
	private ObjectHasSelfGenerator objectHasSelfGenerator;
	@Autowired
	private TransitivePropertyGenerator transitivePropertyGenerator;
	@Autowired
	private ObjectPropertyAssertionGenerator objectPropertyAssertionGenerator;
	@Autowired
	private NegativeObjectPropertyAssertionGenerator negativeObjectPropertyAssertionGenerator;
	@Autowired
	private InverseFunctionalPropertyGenerator inverseFunctionalPropertyGenerator;
	@Autowired
	private SameIndividualGenerator sameIndividualGenerator;
	@Autowired
	private DifferentIndividualsGenerator differentIndividualsGenerator;
	@Autowired
	private AllDisjointObjectPropertiesGenerator allDisjointObjectPropertiesGenerator;
	@Autowired
	private AllDisjointDataPropertiesGenerator allDisjointDataPropertiesGenerator;
	@Autowired
	private EquivalentDataPropertyGenerator equivalentDataPropertyGenerator;
	@Autowired
	private AllDisjointClassesGenerator allDisjointClassesGenerator;
	@Autowired
	private DisjointUnionGenerator disjointUnionGenerator;

	@Test
	void asymmetricThenSymmetricOnTheSamePropertyIsBlocked() {
		GenerationContext ctx = contextFactory.create(1L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 0));

		List<OWLAxiom> asymmetric = asymmetricPropertyGenerator.generate(ctx, 1);
		assertEquals(1, asymmetric.size(), "the only property should be free to become Asymmetric first");
		asymmetric.forEach(axiom -> ctx.ontologyManager().addAxiom(ctx.ontology(), axiom));

		List<OWLAxiom> symmetric = symmetricPropertyGenerator.generate(ctx, 1);
		assertTrue(symmetric.isEmpty(), "Symmetric on the same (only) property must be blocked by the existing Asymmetric axiom");
	}

	@Test
	void reflexiveThenIrreflexiveOnTheSamePropertyIsBlocked() {
		GenerationContext ctx = contextFactory.create(2L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 0));

		List<OWLAxiom> reflexive = reflexivePropertyGenerator.generate(ctx, 1);
		assertEquals(1, reflexive.size());

		List<OWLAxiom> irreflexive = irreflexivePropertyGenerator.generate(ctx, 1);
		assertTrue(irreflexive.isEmpty(), "Irreflexive on the same (only) property must be blocked by the existing Reflexive axiom");
	}

	@Test
	void disjointThenEquivalentOnTheSameClassPairIsBlocked() {
		GenerationContext ctx = contextFactory.create(3L);
		provisioningService.seed(ctx, new EntityCountsDto(2, 0, 0, 0));

		List<OWLAxiom> disjoint = disjointWithGenerator.generate(ctx, 1);
		assertEquals(1, disjoint.size(), "the only class pair should be free to become Disjoint first");

		List<OWLAxiom> equivalent = equivalentClassesGenerator.generate(ctx, 1);
		assertTrue(equivalent.isEmpty(), "EquivalentClasses on the same (only) pair must be blocked by the existing DisjointWith axiom");
	}

	@Test
	void objectPropertyDisjointWithThenEquivalentObjectPropertyOnTheSamePairIsBlocked() {
		GenerationContext ctx = contextFactory.create(4L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 2, 0, 0));

		List<OWLAxiom> disjoint = objectPropertyDisjointWithGenerator.generate(ctx, 1);
		assertEquals(1, disjoint.size(), "the only property pair should be free to become DisjointWith first");

		List<OWLAxiom> equivalent = equivalentObjectPropertyGenerator.generate(ctx, 1);
		assertTrue(equivalent.isEmpty(), "EquivalentObjectProperty on the same (only) pair must be blocked by the existing ObjectPropertyDisjointWith axiom");
	}

	@Test
	void hasSelfThenIrreflexiveOnTheSamePropertyIsBlocked() {
		GenerationContext ctx = contextFactory.create(5L);
		provisioningService.seed(ctx, new EntityCountsDto(1, 1, 0, 0));

		List<OWLAxiom> hasSelf = objectHasSelfGenerator.generate(ctx, 1);
		assertEquals(1, hasSelf.size(), "the only property should be free to be used in ObjectHasSelf first");

		List<OWLAxiom> irreflexive = irreflexivePropertyGenerator.generate(ctx, 1);
		assertTrue(irreflexive.isEmpty(), "Irreflexive on the same (only) property must be blocked by the existing ObjectHasSelf axiom");
	}

	@Test
	void transitiveThenAsymmetricOnTheSamePropertyIsBlocked() {
		GenerationContext ctx = contextFactory.create(6L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 0));

		List<OWLAxiom> transitive = transitivePropertyGenerator.generate(ctx, 1);
		assertEquals(1, transitive.size(), "the only property should be free to become Transitive first");

		List<OWLAxiom> asymmetric = asymmetricPropertyGenerator.generate(ctx, 1);
		assertTrue(asymmetric.isEmpty(),
				"Asymmetric requires a simple property, so it must be blocked by the existing Transitive (non-simple) axiom");
	}

	@Test
	void asymmetricThenTransitiveOnTheSamePropertyIsBlocked() {
		GenerationContext ctx = contextFactory.create(7L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 0));

		List<OWLAxiom> asymmetric = asymmetricPropertyGenerator.generate(ctx, 1);
		assertEquals(1, asymmetric.size(), "the only property should be free to become Asymmetric first");

		List<OWLAxiom> transitive = transitivePropertyGenerator.generate(ctx, 1);
		assertTrue(transitive.isEmpty(),
				"Transitive would make the property non-simple, so it must be blocked by the existing Asymmetric axiom, which requires simplicity");
	}

	@Test
	void positiveThenNegativeObjectPropertyAssertionOnTheSameTripleIsBlocked() {
		GenerationContext ctx = contextFactory.create(8L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 2));

		List<OWLAxiom> positive = objectPropertyAssertionGenerator.generate(ctx, 1);
		assertEquals(1, positive.size(), "the only property/individual-pair combination should be free to be asserted first");

		List<OWLAxiom> negative = negativeObjectPropertyAssertionGenerator.generate(ctx, 1);
		assertTrue(negative.isEmpty(),
				"a negative assertion on the same triple must be blocked by the existing positive assertion");
	}

	@Test
	void transitiveThenObjectPropertyDisjointWithOnTheSamePairIsBlocked() {
		// Regression test: DisjointObjectProperties (the pairwise construct) requires both members to be
		// simple, same as the n-ary AllDisjointObjectProperties construct - this was missed when the
		// non-simple-property guard was first wired up, and reproduced for real via the running app (a
		// property made non-simple by a property chain, then used in a pairwise DisjointObjectProperties
		// axiom, made the whole ontology fail the OWL 2 DL profile check).
		GenerationContext ctx = contextFactory.create(9L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 2, 0, 0));

		List<OWLAxiom> transitive = transitivePropertyGenerator.generate(ctx, 1);
		assertEquals(1, transitive.size(), "one of the two properties should be free to become Transitive first");

		List<OWLAxiom> disjoint = objectPropertyDisjointWithGenerator.generate(ctx, 1);
		assertTrue(disjoint.isEmpty(),
				"ObjectPropertyDisjointWith needs two simple properties, but only one non-Transitive property remains");
	}

	@Test
	void transitiveThenInverseFunctionalOnTheSamePropertyIsBlocked() {
		// Regression test: InverseFunctionalObjectProperty requires a simple property, same as
		// FunctionalObjectProperty - this generator had no guard against non-simple properties at all.
		GenerationContext ctx = contextFactory.create(10L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 0));

		List<OWLAxiom> transitive = transitivePropertyGenerator.generate(ctx, 1);
		assertEquals(1, transitive.size(), "the only property should be free to become Transitive first");

		List<OWLAxiom> inverseFunctional = inverseFunctionalPropertyGenerator.generate(ctx, 1);
		assertTrue(inverseFunctional.isEmpty(),
				"InverseFunctionalObjectProperty requires a simple property, so it must be blocked by the existing Transitive (non-simple) axiom");
	}

	@Test
	void chainOrderRejectsTransitiveCycle() {
		// Regression test: reproduced for real via the running app under a "select everything" stress
		// request, where SubObjectPropertyOf/PropertyChainAxiom picks cascaded into cycles the OWL API's own
		// regularity check rejects outright. Tested directly against the tracker (like the other
		// non-simple/functional tracking tests above) rather than through the generator, since the
		// generator's picks are randomized and can legally re-pick the same non-cyclic pair again.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty p1 = factory.getOWLObjectProperty(IRI.create("urn:test#p1"));
		OWLObjectProperty p2 = factory.getOWLObjectProperty(IRI.create("urn:test#p2"));
		OWLObjectProperty p3 = factory.getOWLObjectProperty(IRI.create("urn:test#p3"));

		assertTrue(tracker.canAddChainOrder(p1, p2));
		tracker.recordChainOrder(p1, p2);
		assertTrue(tracker.canAddChainOrder(p2, p3));
		tracker.recordChainOrder(p2, p3);

		assertFalse(tracker.canAddChainOrder(p3, p1), "adding p3 before p1 would close a transitive cycle through p2");
		assertTrue(tracker.canAddChainOrder(p1, p3), "p1 before p3 is consistent with the existing order, not a cycle");
	}

	@Test
	void chainOrderIsMirroredAcrossDeclaredInverses() {
		// Regression test: reproduced for real via the running app - OWL 2's regularity order must be
		// symmetric under inversion (P before Q implies inverse(P) before inverse(Q)), which a plain
		// non-inverse-aware order graph can't see, letting a cycle slip through the profile checker.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create("urn:test#p"));
		OWLObjectProperty q = factory.getOWLObjectProperty(IRI.create("urn:test#q"));
		OWLObjectProperty invP = factory.getOWLObjectProperty(IRI.create("urn:test#invP"));
		OWLObjectProperty invQ = factory.getOWLObjectProperty(IRI.create("urn:test#invQ"));

		tracker.recordInverse(p, invP);
		tracker.recordInverse(q, invQ);
		tracker.recordChainOrder(p, q);

		assertFalse(tracker.canAddChainOrder(invQ, invP),
				"p before q was mirrored to inverse(p) before inverse(q), so the reverse must be rejected as a cycle");
	}

	@Test
	void nonSimpleMarkingIsMirroredAcrossDeclaredInverses() {
		// Regression test: reproduced for real via the running app - inverse(non-simple property) is itself
		// non-simple per the OWL 2 spec, but markNonSimple only touched the named property, so a property
		// whose *inverse* had been made non-simple elsewhere could still legally pass eligibleForSimpleRequired
		// and get used in a Functional/Irreflexive/cardinality axiom, which the real profile checker rejects.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create("urn:test#p"));
		OWLObjectProperty invP = factory.getOWLObjectProperty(IRI.create("urn:test#invP"));

		tracker.recordInverse(p, invP);
		tracker.markNonSimple(p);

		assertFalse(tracker.eligibleForSimpleRequired(invP), "inverse(p) must also count as non-simple once p is");
	}

	@Test
	void inverseMirroringWorksRegardlessOfDeclarationOrder() {
		// Regression test: reproduced for real via the running app - FunctionalObjectProperty runs (and can
		// mark a property simple-required) before InverseOfProperty in construct generation order, so the
		// write-time mirroring in markSimpleRequired alone missed pairs declared afterward. The eligibility
		// checks must consult the inverse directly so they're correct no matter which axiom came first.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create("urn:test#p"));
		OWLObjectProperty invP = factory.getOWLObjectProperty(IRI.create("urn:test#invP"));

		tracker.markSimpleRequired(p);
		tracker.recordInverse(p, invP);

		assertFalse(tracker.eligibleForNonSimple(invP),
				"invP must be blocked from becoming non-simple even though the inverse pairing was declared after p was marked simple-required");
	}

	@Test
	void nonSimpleMarkingPropagatesUpThePlainSubPropertyChain() {
		// Regression test: found via independent cross-reasoner checking of the real running app - a chain of
		// eleven plain SubObjectPropertyOf edges with a property-chain super-property at the bottom (real
		// case: objectProperty76 through objectProperty86) let the topmost ancestor be used in
		// AllDisjointObjectProperties undetected. OWL 2 DL's global restriction propagates non-simpleness
		// upward through the whole SubObjectPropertyOf hierarchy, not just to directly-marked properties and
		// their declared inverses/equivalents, so eligibleForSimpleRequired must walk that hierarchy too - it
		// previously only checked the property itself.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty[] chain = new OWLObjectProperty[11];
		for (int i = 0; i < chain.length; i++) {
			chain[i] = factory.getOWLObjectProperty(IRI.create("urn:test#p" + i));
		}
		for (int i = 0; i < chain.length - 1; i++) {
			tracker.recordChainOrder(chain[i], chain[i + 1]);
		}

		tracker.markNonSimple(chain[0]);

		assertFalse(tracker.eligibleForSimpleRequired(chain[chain.length - 1]),
				"the topmost ancestor, ten SubObjectPropertyOf hops away, is non-simple per the OWL 2 DL global "
						+ "restriction and must not be usable in a construct requiring a simple property");
		assertFalse(tracker.eligibleForSimpleRequired(chain[5]),
				"an intermediate ancestor must also be recognized as non-simple");

		ConstraintTracker reverseOrder = new ConstraintTracker(List.of());
		OWLObjectProperty base = factory.getOWLObjectProperty(IRI.create("urn:test#base"));
		OWLObjectProperty top = factory.getOWLObjectProperty(IRI.create("urn:test#top"));
		reverseOrder.recordChainOrder(base, top);
		reverseOrder.markSimpleRequired(top);

		assertFalse(reverseOrder.eligibleForNonSimple(base),
				"making the base non-simple would force its already-simple-required ancestor non-simple too, "
						+ "so it must be rejected in the other direction as well");
	}

	@Test
	void propertyCanHaveMultipleDeclaredInverses() {
		// Regression test: reproduced for real via the running app - a property can legally be named as the
		// inverse of more than one other property (multiple InverseObjectProperties axioms), but the tracker
		// originally stored a single Map<P, P> pairing, so the second InverseOf call silently overwrote the
		// first and lost that mirroring relationship entirely.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty p = factory.getOWLObjectProperty(IRI.create("urn:test#p"));
		OWLObjectProperty q = factory.getOWLObjectProperty(IRI.create("urn:test#q"));
		OWLObjectProperty r = factory.getOWLObjectProperty(IRI.create("urn:test#r"));

		tracker.recordInverse(p, q);
		tracker.recordInverse(p, r);
		tracker.markSimpleRequired(p);

		assertFalse(tracker.eligibleForNonSimple(q), "q is still one of p's declared inverses after r was also recorded");
		assertFalse(tracker.eligibleForNonSimple(r), "r is p's second declared inverse and must be tracked too");
	}

	@Test
	void reflexiveThenAsymmetricOnTheSamePropertyIsBlocked() {
		// Regression test: found via a real diagnostic investigation into why "select every construct at
		// default settings" reliably came back INCONSISTENT. Asymmetric implies irreflexive, which directly
		// contradicts Reflexive forcing every individual to relate to itself - unconditionally unsatisfiable,
		// yet the registry only had Reflexive/Irreflexive, not Reflexive/Asymmetric.
		GenerationContext ctx = contextFactory.create(12L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 1, 0, 0));

		List<OWLAxiom> reflexive = reflexivePropertyGenerator.generate(ctx, 1);
		assertEquals(1, reflexive.size(), "the only property should be free to become Reflexive first");

		List<OWLAxiom> asymmetric = asymmetricPropertyGenerator.generate(ctx, 1);
		assertTrue(asymmetric.isEmpty(), "Asymmetric on the same (only) property must be blocked by the existing Reflexive axiom");
	}

	@Test
	void sameIndividualThenDifferentIndividualsOnTheSamePairIsBlocked() {
		// Regression test: SameIndividualGenerator/DifferentIndividualsGenerator had zero coordination with
		// each other, found by the same diagnostic investigation as above.
		GenerationContext ctx = contextFactory.create(13L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 0, 0, 2));

		List<OWLAxiom> same = sameIndividualGenerator.generate(ctx, 1);
		assertEquals(1, same.size(), "the only individual pair should be free to become Same first");

		List<OWLAxiom> different = differentIndividualsGenerator.generate(ctx, 1);
		assertTrue(different.isEmpty(), "DifferentIndividuals on the same (only) pair must be blocked by the existing Same axiom");
	}

	@Test
	void sameIndividualTransitiveClosureBlocksDerivedDifferent() {
		// Regression test: the real inconsistency found in production was three hops apart - Same(a,b),
		// Same(a,c), then Different(b,c) - b and c were never directly related, but sameAs is transitive so
		// they're forced equal anyway. A tracker that only remembers direct pairs can't see this.
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLNamedIndividual a = factory.getOWLNamedIndividual(IRI.create("urn:test#a"));
		OWLNamedIndividual b = factory.getOWLNamedIndividual(IRI.create("urn:test#b"));
		OWLNamedIndividual c = factory.getOWLNamedIndividual(IRI.create("urn:test#c"));

		assertTrue(tracker.canAssertSame(a, b));
		tracker.recordSame(a, b);
		assertTrue(tracker.canAssertSame(a, c));
		tracker.recordSame(a, c);

		assertFalse(tracker.canAssertDifferent(b, c),
				"b and c are transitively forced equal through a, so Different(b,c) must be blocked even though they were never asserted Same directly");
	}

	@Test
	void differentIndividualsBlocksSubsequentSameAcrossTheGroup() {
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLNamedIndividual a = factory.getOWLNamedIndividual(IRI.create("urn:test#a"));
		OWLNamedIndividual b = factory.getOWLNamedIndividual(IRI.create("urn:test#b"));
		OWLNamedIndividual c = factory.getOWLNamedIndividual(IRI.create("urn:test#c"));

		tracker.recordDifferent(a, b);
		tracker.recordSame(a, c);

		assertFalse(tracker.canAssertSame(b, c),
				"c joined a's same-as group, and a is already asserted Different from b, so Same(b,c) must be blocked");
	}

	@Test
	void allDisjointObjectPropertiesAvoidsGroupContainingAnEquivalentPair() {
		// Regression test: the n-ary AllDisjointObjectProperties construct never checked the pairwise
		// EquivalentObjectProperty clash at all - found as one of the three real, independent causes of the
		// "select everything at defaults" inconsistency. With exactly 3 properties and one pair already
		// equivalent, no clash-free group of 3 exists among the original pool, so a 4th must get minted.
		GenerationContext ctx = contextFactory.create(14L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 3, 0, 0));

		List<OWLAxiom> equivalent = equivalentObjectPropertyGenerator.generate(ctx, 1);
		assertEquals(1, equivalent.size(), "some pair among the 3 properties should be free to become equivalent first");
		assertEquals(3, ctx.pools().objectProperties().size());

		List<OWLAxiom> allDisjoint = allDisjointObjectPropertiesGenerator.generate(ctx, 1);
		assertEquals(1, allDisjoint.size());
		assertEquals(4, ctx.pools().objectProperties().size(),
				"no clash-free group of 3 exists among the original 3 properties once one pair is equivalent, so a 4th must be minted");
	}

	@Test
	void allDisjointDataPropertiesAvoidsGroupContainingAnEquivalentPair() {
		GenerationContext ctx = contextFactory.create(15L);
		provisioningService.seed(ctx, new EntityCountsDto(0, 0, 3, 0));

		List<OWLAxiom> equivalent = equivalentDataPropertyGenerator.generate(ctx, 1);
		assertEquals(1, equivalent.size(), "some pair among the 3 properties should be free to become equivalent first");
		assertEquals(3, ctx.pools().dataProperties().size());

		List<OWLAxiom> allDisjoint = allDisjointDataPropertiesGenerator.generate(ctx, 1);
		assertEquals(1, allDisjoint.size());
		assertEquals(4, ctx.pools().dataProperties().size(),
				"no clash-free group of 3 exists among the original 3 properties once one pair is equivalent, so a 4th must be minted");
	}

	@Test
	void allDisjointClassesAvoidsGroupContainingAnEquivalentPair() {
		GenerationContext ctx = contextFactory.create(16L);
		provisioningService.seed(ctx, new EntityCountsDto(3, 0, 0, 0));

		List<OWLAxiom> equivalent = equivalentClassesGenerator.generate(ctx, 1);
		assertEquals(1, equivalent.size(), "some pair among the 3 classes should be free to become equivalent first");
		assertEquals(3, ctx.pools().classes().size());

		List<OWLAxiom> allDisjoint = allDisjointClassesGenerator.generate(ctx, 1);
		assertEquals(1, allDisjoint.size());
		assertEquals(4, ctx.pools().classes().size(),
				"no clash-free group of 3 exists among the original 3 classes once one pair is equivalent, so a 4th must be minted");
	}

	@Test
	void equivalentObjectPropertyTransitiveClosureBlocksDisjoint() {
		// Regression test: the earlier pairwise-only EquivalentObjectProperty tracking recorded (a,b) and
		// (b,c) as two separate facts, never noticing that transitivity forces a and c equivalent too - found
		// as part of the same "select everything at defaults" diagnostic investigation as the fixes above.
		ConstraintTracker tracker = new ConstraintTracker(
				List.of(new ClashRule(ConstructId.OBJECT_PROPERTY_DISJOINT_WITH, ConstructId.EQUIVALENT_OBJECT_PROPERTY)));
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty a = factory.getOWLObjectProperty(IRI.create("urn:test#a"));
		OWLObjectProperty b = factory.getOWLObjectProperty(IRI.create("urn:test#b"));
		OWLObjectProperty c = factory.getOWLObjectProperty(IRI.create("urn:test#c"));

		assertTrue(tracker.canMergeEquivalentObjectProperties(a, b));
		tracker.mergeEquivalentObjectProperties(a, b);
		assertTrue(tracker.canMergeEquivalentObjectProperties(b, c));
		tracker.mergeEquivalentObjectProperties(b, c);

		assertFalse(tracker.canDeclareDisjointObjectProperties(a, c),
				"a and c are transitively forced equivalent through b, so DisjointObjectProperties(a,c) must be blocked");
	}

	@Test
	void inverseOfSharedPartnerForcesEquivalenceAndBlocksDisjoint() {
		// Regression test: reproduced for real as one of the minimal inconsistency cores found in production -
		// two different properties both declared the inverse of a third are forced equivalent (inverse is a
		// well-defined, involutive operation), which the InverseOf generator never checked at all.
		ConstraintTracker tracker = new ConstraintTracker(
				List.of(new ClashRule(ConstructId.OBJECT_PROPERTY_DISJOINT_WITH, ConstructId.EQUIVALENT_OBJECT_PROPERTY)));
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty a = factory.getOWLObjectProperty(IRI.create("urn:test#a"));
		OWLObjectProperty b = factory.getOWLObjectProperty(IRI.create("urn:test#b"));
		OWLObjectProperty c = factory.getOWLObjectProperty(IRI.create("urn:test#c"));

		assertTrue(tracker.canDeclareInverse(a, b));
		tracker.recordInverse(a, b);
		assertTrue(tracker.canDeclareInverse(c, b));
		tracker.recordInverse(c, b);

		assertFalse(tracker.canDeclareDisjointObjectProperties(a, c),
				"a and c are both declared inverse of b, forcing them equivalent, so DisjointObjectProperties(a,c) must be blocked");
	}

	@Test
	void disjointUnionAvoidsMainOrPartsAlreadyEquivalent() {
		// Regression test: DisjointUnionGenerator implies main ≡ (part1 ⊔ part2) with the parts pairwise
		// disjoint, but never checked whether main or the parts were already forced equivalent to each other -
		// found as one of the minimal inconsistency cores in production. With exactly 3 classes and one pair
		// already equivalent, DisjointUnion's forced "main + the other two" selection can't avoid a clash
		// (whichever class becomes main, the clash lands either between main and a part, or between the two
		// parts), so a 4th class must get minted.
		GenerationContext ctx = contextFactory.create(17L);
		provisioningService.seed(ctx, new EntityCountsDto(3, 0, 0, 0));

		List<OWLAxiom> equivalent = equivalentClassesGenerator.generate(ctx, 1);
		assertEquals(1, equivalent.size(), "some pair among the 3 classes should be free to become equivalent first");
		assertEquals(3, ctx.pools().classes().size());

		List<OWLAxiom> disjointUnion = disjointUnionGenerator.generate(ctx, 1);
		assertEquals(1, disjointUnion.size());
		assertEquals(4, ctx.pools().classes().size(),
				"no clash-free main+2-parts selection exists among the original 3 classes once one pair is equivalent, so a 4th must be minted");
	}

	@Test
	void functionalPropertyTrackingIsQueryableDirectly() {
		// Direct unit-level check of the new tracked state (cardinality in the real generators is randomized,
		// so this is more reliable than trying to force a specific cardinality through the full pipeline).
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty objectProperty = factory.getOWLObjectProperty(IRI.create("urn:test#p"));
		OWLDataProperty dataProperty = factory.getOWLDataProperty(IRI.create("urn:test#d"));

		assertFalse(tracker.isFunctional(objectProperty));
		tracker.markFunctional(objectProperty);
		assertTrue(tracker.isFunctional(objectProperty));

		assertFalse(tracker.isFunctionalData(dataProperty));
		tracker.markFunctionalData(dataProperty);
		assertTrue(tracker.isFunctionalData(dataProperty));
	}

	@Test
	void nonSimpleAndSimpleRequiredTrackingAreMutuallyExclusive() {
		ConstraintTracker tracker = new ConstraintTracker(List.of());
		OWLDataFactory factory = OWLManager.createOWLOntologyManager().getOWLDataFactory();
		OWLObjectProperty property = factory.getOWLObjectProperty(IRI.create("urn:test#p"));

		assertTrue(tracker.eligibleForNonSimple(property));
		assertTrue(tracker.eligibleForSimpleRequired(property));

		tracker.markSimpleRequired(property);
		assertFalse(tracker.eligibleForNonSimple(property), "a property already required to stay simple can't become non-simple");

		ConstraintTracker tracker2 = new ConstraintTracker(List.of());
		tracker2.markNonSimple(property);
		assertFalse(tracker2.eligibleForSimpleRequired(property), "a non-simple property can't be used somewhere requiring simplicity");
	}
}
