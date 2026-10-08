package com.owl2gendl.constraint;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import com.owl2gendl.catalog.ConstructId;

/**
 * Per-generation-run state tracking which construct has already been applied to which entity (or entity
 * tuple), and whether a new construct application would clash per {@link ConstraintRuleRegistry}. Scoped to
 * one {@code GenerationContext} — never shared across concurrent runs, unlike the old codebase's
 * {@code public static} equivalents.
 *
 * <p>Two clash shapes don't fit the pairwise {@link ClashRule} model (which only tracks "has construct X
 * already applied to this exact entity/tuple") and are tracked here directly instead:
 *
 * <p><b>Property simplicity.</b> OWL 2 DL structurally forbids a "non-simple" object property (one marked
 * transitive, or a super-property of a property chain) from appearing in a cardinality restriction,
 * {@code ObjectHasSelf}, {@code Irreflexive}, {@code Asymmetric}, {@code FunctionalObjectProperty}, or
 * {@code DisjointObjectProperties} — this is a syntactic validity requirement, not just a soundness
 * nicety, so it's tracked bidirectionally: a property already used somewhere requiring simplicity can't
 * later be made non-simple, and vice versa. Non-simpleness also propagates <em>upward</em> through the
 * {@code SubObjectPropertyOf}/property-chain hierarchy — if {@code P} is non-simple and {@code P ⊑ Q}, then
 * {@code Q} is non-simple too, transitively up every ancestor (and each ancestor's own declared inverses and
 * {@code EquivalentObjectProperties} group). {@link #markNonSimple} and {@link #eligibleForNonSimple} walk
 * this closure using the same {@link #chainOrderSuccessors} graph {@code PropertyChainAxiomGenerator} and
 * {@code SubObjectPropertyOfGenerator} already populate for regularity checking, rather than a separate
 * structure — a real, previously-shipped gap here (only direct inverses/equivalents were propagated, not the
 * super-property chain) let generated ontologies reference an effectively-non-simple property in
 * {@code DisjointObjectProperties} undetected; confirmed reproducible and root-caused via independent
 * cross-reasoner checking (HermiT rejects the resulting ontology at load time; Openllet and Konclude both
 * tolerate it silently).
 *
 * <p><b>Functional properties vs. cardinality ≥ 2.</b> A property declared {@code Functional} allows at
 * most one value per subject, which directly contradicts a {@code MinCardinality}/{@code ExactCardinality}
 * (qualified or not) restriction requiring ≥ 2 on the same property.
 *
 * <p><b>Property-chain regularity.</b> OWL 2 DL requires the role hierarchy formed by
 * {@code SubObjectPropertyOf} axioms (plain and chain) to be "regular": there must exist a strict partial
 * order on properties consistent with every edge (chain factor ≺ chain super-property, or sub-property ≺
 * super-property), and per the OWL 2 spec that order must also be symmetric under inversion - if
 * {@code P ≺ Q} then {@code inverse(P) ≺ inverse(Q)} too. Tracked here as a directed graph of "must come
 * before" edges (mirrored across any declared {@code InverseObjectProperties} pairs at the point each edge
 * is recorded); a new edge is only accepted if the property it points *from* isn't already reachable *from*
 * the property it points *to* - otherwise it would close a cycle, which the real OWL API profile checker
 * rejects outright.
 *
 * <p><b>SameIndividual / DifferentIndividuals transitive closure.</b> {@code sameAs} is transitive, so
 * {@code Same(a,b)} plus {@code Same(a,c)} forces {@code b} and {@code c} to denote the same individual too
 * - a later {@code Different(b,c)} is a contradiction even though {@code b} and {@code c} were never directly
 * related by name. Tracked as same-as groups (union-find via shared {@code Set} references) plus a flat set
 * of asserted-different pairs; every cross-pair between two groups is checked before merging them.
 *
 * <p><b>EquivalentClasses / EquivalentObjectProperty / EquivalentDataProperty transitive closure.</b> Same
 * shape as SameIndividual above: {@code Equivalent(a,b)} plus {@code Equivalent(b,c)} forces {@code a} and
 * {@code c} equivalent too, even though never asserted directly - a later {@code Disjoint(a,c)} must be
 * rejected. Tracked as union-find groups, checked against the existing pairwise Disjoint/Equivalent
 * {@link ClashRule}s (every cross-pair between the two groups being merged must itself be clash-free).
 * {@code InverseObjectProperties} adds a further wrinkle unique to object properties: if two different
 * properties are both declared the inverse of the same third property, they must denote the same relation
 * (inverse is a well-defined, involutive operation) - so a new {@code InverseObjectProperties(a,b)} axiom
 * merges {@code a} into the equivalence group of every property already declared inverse of {@code b} (and
 * vice versa), the same way an explicit {@code EquivalentObjectProperty} axiom would.
 */
public class ConstraintTracker {

	private final List<ClashRule> rules;
	private final Map<Set<OWLEntity>, Set<ConstructId>> applied = new HashMap<>();
	private final Set<OWLObjectProperty> nonSimpleProperties = new HashSet<>();
	private final Set<OWLObjectProperty> simpleRequiredProperties = new HashSet<>();
	private final Set<OWLObjectProperty> functionalObjectProperties = new HashSet<>();
	private final Set<OWLDataProperty> functionalDataProperties = new HashSet<>();
	private final Map<OWLObjectProperty, Set<OWLObjectProperty>> chainOrderSuccessors = new HashMap<>();
	// A property can legally have more than one declared inverse (multiple InverseObjectProperties axioms
	// naming it) - a plain Map<P, P> silently drops all but the last one recorded.
	private final Map<OWLObjectProperty, Set<OWLObjectProperty>> declaredInverses = new HashMap<>();
	private final Map<OWLNamedIndividual, Set<OWLNamedIndividual>> sameAsGroups = new HashMap<>();
	private final Set<Set<OWLNamedIndividual>> differentPairs = new HashSet<>();
	private final Map<OWLClass, Set<OWLClass>> equivalentClassGroups = new HashMap<>();
	private final Map<OWLObjectProperty, Set<OWLObjectProperty>> equivalentObjectPropertyGroups = new HashMap<>();
	private final Map<OWLDataProperty, Set<OWLDataProperty>> equivalentDataPropertyGroups = new HashMap<>();

	public ConstraintTracker(List<ClashRule> rules) {
		this.rules = rules;
	}

	/** True if applying {@code constructId} to this exact entity/tuple would not clash with what's already there. */
	public boolean canApply(ConstructId constructId, OWLEntity... entities) {
		Set<ConstructId> existing = applied.getOrDefault(Set.of(entities), Set.of());
		for (ClashRule rule : rules) {
			if (rule.first() == constructId && existing.contains(rule.second())) {
				return false;
			}
			if (rule.second() == constructId && existing.contains(rule.first())) {
				return false;
			}
		}
		return true;
	}

	public void recordApplied(ConstructId constructId, OWLEntity... entities) {
		applied.computeIfAbsent(Set.of(entities), key -> new HashSet<>()).add(constructId);
	}

	/**
	 * Every property whose simplicity status is forced to match {@code property}'s: its declared inverses
	 * (transitivity of a relation implies transitivity of its inverse) and its {@code EquivalentObjectProperties}
	 * group (two properties declared equivalent must denote the same relation, so one can't be simple while
	 * the other isn't). Used by both directions of the simple/non-simple tracking below so that whichever
	 * axiom - the {@code TransitiveObjectProperty}/chain declaration, the {@code EquivalentObjectProperties}
	 * merge, or the simplicity-requiring construct (e.g. {@code ObjectHasSelf}) - happens to be generated last
	 * still sees a consistent view, regardless of generation order.
	 */
	private Set<OWLObjectProperty> simplicityLinked(OWLObjectProperty property) {
		Set<OWLObjectProperty> linked = new HashSet<>(declaredInverses.getOrDefault(property, Set.of()));
		linked.addAll(equivalentGroupOf(property));
		linked.remove(property);
		return linked;
	}

	/**
	 * Every property that becomes (or already is) non-simple as a consequence of {@code property} being
	 * non-simple: {@code property} itself, everything {@link #simplicityLinked} to it, and — per the OWL 2 DL
	 * global restriction that non-simpleness propagates upward through the role hierarchy — every ancestor
	 * reachable via {@link #chainOrderSuccessors} (a plain sub-property or chain-factor edge always points
	 * from the "simpler" side toward its super-property), with each ancestor's own simplicity-linked
	 * properties folded in too. A breadth-first walk over both edge kinds together, so the full transitive
	 * closure is computed regardless of how deep the sub-property chain or how many inverse/equivalence hops
	 * are involved.
	 */
	private Set<OWLObjectProperty> nonSimpleClosure(OWLObjectProperty property) {
		Set<OWLObjectProperty> closure = new HashSet<>();
		Deque<OWLObjectProperty> queue = new ArrayDeque<>();
		queue.add(property);
		while (!queue.isEmpty()) {
			OWLObjectProperty current = queue.poll();
			if (!closure.add(current)) {
				continue;
			}
			queue.addAll(simplicityLinked(current));
			queue.addAll(chainOrderSuccessors.getOrDefault(current, Set.of()));
		}
		return closure;
	}

	/**
	 * True if {@code property} isn't already required to stay simple, i.e. it's safe to make non-simple.
	 * Checks the full {@link #nonSimpleClosure} — declared inverses, the {@code EquivalentObjectProperties}
	 * group, and every ancestor up the sub-property/chain hierarchy — not just {@code property} itself, so
	 * this stays correct regardless of the order constructs happen to be generated in.
	 */
	public boolean eligibleForNonSimple(OWLObjectProperty property) {
		return nonSimpleClosure(property).stream().noneMatch(simpleRequiredProperties::contains);
	}

	public void markNonSimple(OWLObjectProperty property) {
		nonSimpleProperties.addAll(nonSimpleClosure(property));
	}

	/**
	 * True if {@code property} isn't already non-simple, i.e. it's safe to use somewhere requiring
	 * simplicity. Checks declared inverses and the {@code EquivalentObjectProperties} group too, for the same
	 * order-independence reason as {@link #eligibleForNonSimple}.
	 */
	public boolean eligibleForSimpleRequired(OWLObjectProperty property) {
		if (nonSimpleProperties.contains(property)) {
			return false;
		}
		return simplicityLinked(property).stream().noneMatch(nonSimpleProperties::contains);
	}

	public void markSimpleRequired(OWLObjectProperty property) {
		simpleRequiredProperties.add(property);
		simpleRequiredProperties.addAll(simplicityLinked(property));
	}

	public void markFunctional(OWLObjectProperty property) {
		functionalObjectProperties.add(property);
	}

	public boolean isFunctional(OWLObjectProperty property) {
		return functionalObjectProperties.contains(property);
	}

	public void markFunctionalData(OWLDataProperty property) {
		functionalDataProperties.add(property);
	}

	public boolean isFunctionalData(OWLDataProperty property) {
		return functionalDataProperties.contains(property);
	}

	/** True if recording {@code before ≺ after} would not close a cycle in the existing order graph. */
	public boolean canAddChainOrder(OWLObjectProperty before, OWLObjectProperty after) {
		if (before.equals(after)) {
			return false;
		}
		Set<OWLObjectProperty> visited = new HashSet<>();
		Deque<OWLObjectProperty> queue = new ArrayDeque<>();
		queue.add(after);
		while (!queue.isEmpty()) {
			OWLObjectProperty current = queue.poll();
			if (current.equals(before)) {
				return false; // "after" can already reach "before" - adding before->after would close a cycle
			}
			if (visited.add(current)) {
				queue.addAll(chainOrderSuccessors.getOrDefault(current, Set.of()));
			}
		}
		return true;
	}

	public void recordChainOrder(OWLObjectProperty before, OWLObjectProperty after) {
		chainOrderSuccessors.computeIfAbsent(before, key -> new HashSet<>()).add(after);
		for (OWLObjectProperty invBefore : declaredInverses.getOrDefault(before, Set.of())) {
			for (OWLObjectProperty invAfter : declaredInverses.getOrDefault(after, Set.of())) {
				chainOrderSuccessors.computeIfAbsent(invBefore, key -> new HashSet<>()).add(invAfter);
			}
		}
	}

	/**
	 * True if declaring {@code a} the inverse of {@code b} would be safe: neither property already has a
	 * *different* declared inverse that couldn't legally be merged into an equivalence group with the new
	 * partner (see the class-level doc on why a shared inverse partner forces equivalence).
	 */
	public boolean canDeclareInverse(OWLObjectProperty a, OWLObjectProperty b) {
		for (OWLObjectProperty other : declaredInverses.getOrDefault(b, Set.of())) {
			if (!other.equals(a) && !canMergeEquivalentObjectProperties(a, other)) {
				return false;
			}
		}
		for (OWLObjectProperty other : declaredInverses.getOrDefault(a, Set.of())) {
			if (!other.equals(b) && !canMergeEquivalentObjectProperties(b, other)) {
				return false;
			}
		}
		return true;
	}

	/** Records {@code a}/{@code b} as a declared {@code InverseObjectProperties} pair for order mirroring. */
	public void recordInverse(OWLObjectProperty a, OWLObjectProperty b) {
		for (OWLObjectProperty other : declaredInverses.getOrDefault(b, Set.of())) {
			if (!other.equals(a)) {
				mergeEquivalentObjectProperties(a, other);
			}
		}
		for (OWLObjectProperty other : declaredInverses.getOrDefault(a, Set.of())) {
			if (!other.equals(b)) {
				mergeEquivalentObjectProperties(b, other);
			}
		}
		declaredInverses.computeIfAbsent(a, key -> new HashSet<>()).add(b);
		declaredInverses.computeIfAbsent(b, key -> new HashSet<>()).add(a);
	}

	private Set<OWLNamedIndividual> sameAsGroupOf(OWLNamedIndividual individual) {
		return sameAsGroups.getOrDefault(individual, Set.of(individual));
	}

	/** True if merging {@code a}'s and {@code b}'s same-as groups wouldn't force a Different pair to be equal. */
	public boolean canAssertSame(OWLNamedIndividual a, OWLNamedIndividual b) {
		for (OWLNamedIndividual x : sameAsGroupOf(a)) {
			for (OWLNamedIndividual y : sameAsGroupOf(b)) {
				if (!x.equals(y) && differentPairs.contains(Set.of(x, y))) {
					return false;
				}
			}
		}
		return true;
	}

	public void recordSame(OWLNamedIndividual a, OWLNamedIndividual b) {
		Set<OWLNamedIndividual> merged = new HashSet<>(sameAsGroupOf(a));
		merged.addAll(sameAsGroupOf(b));
		for (OWLNamedIndividual member : merged) {
			sameAsGroups.put(member, merged);
		}
	}

	/** True if {@code a} and {@code b} aren't already forced equal by the transitive closure of asserted Same. */
	public boolean canAssertDifferent(OWLNamedIndividual a, OWLNamedIndividual b) {
		return !sameAsGroupOf(a).contains(b);
	}

	public void recordDifferent(OWLNamedIndividual a, OWLNamedIndividual b) {
		differentPairs.add(Set.of(a, b));
	}

	private Set<OWLClass> equivalentGroupOf(OWLClass clazz) {
		return equivalentClassGroups.getOrDefault(clazz, Set.of(clazz));
	}

	/** True if merging {@code a}'s and {@code b}'s equivalence groups wouldn't force a Disjoint pair equal. */
	public boolean canMergeEquivalentClasses(OWLClass a, OWLClass b) {
		for (OWLClass x : equivalentGroupOf(a)) {
			for (OWLClass y : equivalentGroupOf(b)) {
				if (!x.equals(y) && !canApply(ConstructId.EQUIVALENT_CLASSES, x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	public void mergeEquivalentClasses(OWLClass a, OWLClass b) {
		Set<OWLClass> merged = new HashSet<>(equivalentGroupOf(a));
		merged.addAll(equivalentGroupOf(b));
		for (OWLClass member : merged) {
			equivalentClassGroups.put(member, merged);
		}
	}

	/** True if {@code a}/{@code b} aren't already forced equal by the transitive closure of asserted Equivalent. */
	public boolean canDeclareDisjointClasses(OWLClass a, OWLClass b) {
		return !equivalentGroupOf(a).contains(b);
	}

	private Set<OWLObjectProperty> equivalentGroupOf(OWLObjectProperty property) {
		return equivalentObjectPropertyGroups.getOrDefault(property, Set.of(property));
	}

	/**
	 * Whether either side of the merge is already non-simple (transitive, or a property-chain super-property)
	 * while the other is already simple-required (used somewhere like {@code ObjectHasSelf} that forbids
	 * non-simple properties). Declaring them equivalent would make the simple-required side non-simple too,
	 * which is exactly the OWL 2 DL global-restriction violation this tracker exists to prevent.
	 */
	private boolean simplicityCompatible(OWLObjectProperty a, OWLObjectProperty b) {
		boolean aNonSimple = equivalentGroupOf(a).stream().anyMatch(nonSimpleProperties::contains);
		boolean bNonSimple = equivalentGroupOf(b).stream().anyMatch(nonSimpleProperties::contains);
		boolean aSimpleRequired = equivalentGroupOf(a).stream().anyMatch(simpleRequiredProperties::contains);
		boolean bSimpleRequired = equivalentGroupOf(b).stream().anyMatch(simpleRequiredProperties::contains);
		return !((aNonSimple && bSimpleRequired) || (bNonSimple && aSimpleRequired));
	}

	public boolean canMergeEquivalentObjectProperties(OWLObjectProperty a, OWLObjectProperty b) {
		if (!simplicityCompatible(a, b)) {
			return false;
		}
		for (OWLObjectProperty x : equivalentGroupOf(a)) {
			for (OWLObjectProperty y : equivalentGroupOf(b)) {
				if (!x.equals(y) && !canApply(ConstructId.EQUIVALENT_OBJECT_PROPERTY, x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	public void mergeEquivalentObjectProperties(OWLObjectProperty a, OWLObjectProperty b) {
		Set<OWLObjectProperty> merged = new HashSet<>(equivalentGroupOf(a));
		merged.addAll(equivalentGroupOf(b));
		boolean nonSimple = merged.stream().anyMatch(nonSimpleProperties::contains);
		boolean simpleRequired = merged.stream().anyMatch(simpleRequiredProperties::contains);
		for (OWLObjectProperty member : merged) {
			equivalentObjectPropertyGroups.put(member, merged);
		}
		// Re-mark through the now-merged group so every member's status (and, transitively, its own declared
		// inverses) is consistent, mirroring what markNonSimple/markSimpleRequired would do if called fresh.
		if (nonSimple) {
			merged.forEach(this::markNonSimple);
		}
		if (simpleRequired) {
			merged.forEach(this::markSimpleRequired);
		}
	}

	public boolean canDeclareDisjointObjectProperties(OWLObjectProperty a, OWLObjectProperty b) {
		return !equivalentGroupOf(a).contains(b);
	}

	private Set<OWLDataProperty> equivalentGroupOf(OWLDataProperty property) {
		return equivalentDataPropertyGroups.getOrDefault(property, Set.of(property));
	}

	public boolean canMergeEquivalentDataProperties(OWLDataProperty a, OWLDataProperty b) {
		for (OWLDataProperty x : equivalentGroupOf(a)) {
			for (OWLDataProperty y : equivalentGroupOf(b)) {
				if (!x.equals(y) && !canApply(ConstructId.EQUIVALENT_DATA_PROPERTY, x, y)) {
					return false;
				}
			}
		}
		return true;
	}

	public void mergeEquivalentDataProperties(OWLDataProperty a, OWLDataProperty b) {
		Set<OWLDataProperty> merged = new HashSet<>(equivalentGroupOf(a));
		merged.addAll(equivalentGroupOf(b));
		for (OWLDataProperty member : merged) {
			equivalentDataPropertyGroups.put(member, merged);
		}
	}

	public boolean canDeclareDisjointDataProperties(OWLDataProperty a, OWLDataProperty b) {
		return !equivalentGroupOf(a).contains(b);
	}
}
