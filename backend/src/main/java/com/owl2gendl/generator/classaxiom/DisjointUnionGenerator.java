package com.owl2gendl.generator.classaxiom;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.springframework.stereotype.Component;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;
import com.owl2gendl.generator.ConstructGenerator;

/**
 * {@code mainClass ≡ partA ⊔ partB}, with {@code partA} and {@code partB} pairwise disjoint. If
 * {@code mainClass} is already (possibly transitively) equivalent to one of its own parts, or two parts are
 * already equivalent to each other, the implicit "main ≡ union of pairwise-disjoint parts" semantics forces
 * at least one part to be empty - combined with something elsewhere forcing that part non-empty (a
 * ClassAssertion, a cardinality restriction, ...), that's an unconditional clash. Reproduced for real as one
 * of the minimal inconsistency cores found in production; {@link com.owl2gendl.constraint.ConstraintTracker}
 * already tracks the transitive equivalence closure needed to guard against it (see
 * {@code canDeclareDisjointClasses}).
 */
@Component
public class DisjointUnionGenerator implements ConstructGenerator {

	private static final int MAX_ATTEMPTS_PER_MEMBER = 8;

	@Override
	public ConstructId id() {
		return ConstructId.DISJOINT_UNION;
	}

	@Override
	public List<OWLAxiom> generate(GenerationContext ctx, int count) {
		List<OWLAxiom> axioms = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			OWLClass main = ctx.pools().classes().pick(ctx.random());
			Set<OWLClass> parts = new LinkedHashSet<>();
			while (parts.size() < 2) {
				OWLClass candidate = null;
				for (int attempt = 0; attempt < MAX_ATTEMPTS_PER_MEMBER; attempt++) {
					OWLClass pick = ctx.pools().classes().pickDifferentFrom(ctx.random(), main);
					boolean eligible = !parts.contains(pick)
							&& ctx.constraintTracker().canDeclareDisjointClasses(main, pick)
							&& parts.stream().allMatch(existing -> ctx.constraintTracker().canApply(ConstructId.DISJOINT_WITH, pick, existing)
									&& ctx.constraintTracker().canDeclareDisjointClasses(pick, existing));
					if (eligible) {
						candidate = pick;
						break;
					}
				}
				parts.add(candidate != null ? candidate : ctx.pools().classes().mintNew());
			}
			axioms.add(ctx.dataFactory().getOWLDisjointUnionAxiom(main, Set.<OWLClassExpression>copyOf(parts)));
			// DisjointUnion implies the parts are pairwise disjoint, same as an explicit DisjointWith axiom
			// would — record it as such so a later EquivalentClasses on the same pair is correctly blocked
			// by the existing DisjointWith/EquivalentClasses rule, without needing a separate rule for this.
			List<OWLClass> partList = List.copyOf(parts);
			for (int a = 0; a < partList.size(); a++) {
				for (int b = a + 1; b < partList.size(); b++) {
					ctx.constraintTracker().recordApplied(ConstructId.DISJOINT_WITH, partList.get(a), partList.get(b));
				}
			}
		}
		return axioms;
	}
}
