package com.owl2gendl.topology;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

import com.owl2gendl.context.GenerationContext;

/**
 * Tracks each class's depth and child count (scoped to one generation run) and biases superclass choice
 * toward classes still under the target depth and branching factor. Falls back to any different pooled
 * class (or a freshly minted one) once nothing eligible remains, so generation never stalls.
 *
 * <p>Which eligible candidate gets chosen is delegated to the class pool's configured
 * {@link com.owl2gendl.topology.AttachmentStrategy} (via {@link com.owl2gendl.pool.EntityPool#chooseAmong}),
 * the same one the request's topology variant wires up everywhere else — so Chain genuinely extends the
 * newest eligible branch, Balanced Tree genuinely spreads new children across the least-used eligible
 * superclasses, and so on, instead of every variant picking uniformly at random among eligible superclasses
 * regardless of which strategy was requested.
 *
 * <p>Because classes are reused across multiple {@code SubClassOf} axioms, a candidate superclass can
 * already be a descendant of the class being processed (via an earlier axiom) — picking it anyway would
 * silently close a cycle in the asserted hierarchy. {@link #wouldCreateCycle} guards against this in both
 * the depth/branching-constrained choice and the fallback path.
 */
public class DepthBiasedHierarchyStrategy implements HierarchyStrategy {

	private final int targetDepth;
	private final int branchingFactor;
	private final Map<OWLClass, Integer> depth = new HashMap<>();
	private final Map<OWLClass, Integer> childCount = new HashMap<>();
	private final Map<OWLClass, List<OWLClass>> parentsOf = new HashMap<>();

	public DepthBiasedHierarchyStrategy(int targetDepth, int branchingFactor) {
		this.targetDepth = Math.max(1, targetDepth);
		this.branchingFactor = Math.max(1, branchingFactor);
	}

	@Override
	public OWLClass chooseSuperclass(GenerationContext ctx, OWLClass subclass) {
		List<OWLClass> pooled = ctx.pools().classes().all();

		List<OWLClass> eligible = pooled.stream()
				.filter(candidate -> !candidate.equals(subclass))
				.filter(candidate -> depth.getOrDefault(candidate, 0) < targetDepth)
				.filter(candidate -> childCount.getOrDefault(candidate, 0) < branchingFactor)
				.filter(candidate -> !wouldCreateCycle(subclass, candidate))
				.toList();

		OWLClass chosen;
		if (!eligible.isEmpty()) {
			chosen = ctx.pools().classes().chooseAmong(eligible, ctx.random());
		} else {
			List<OWLClass> safeFallback = pooled.stream()
					.filter(candidate -> !candidate.equals(subclass))
					.filter(candidate -> !wouldCreateCycle(subclass, candidate))
					.toList();
			chosen = safeFallback.isEmpty() ? ctx.pools().classes().mintNew() : chooseShallowest(ctx, safeFallback);
		}

		parentsOf.computeIfAbsent(subclass, k -> new ArrayList<>()).add(chosen);
		childCount.merge(chosen, 1, Integer::sum);
		int chosenDepth = depth.getOrDefault(chosen, 0);
		depth.merge(subclass, chosenDepth + 1, Math::max);
		return chosen;
	}

	/**
	 * Once no candidate satisfies both the depth and branching targets, the safest way to keep growing the
	 * hierarchy without letting depth run away is to still prefer the currently-shallowest safe candidates —
	 * {@code depth} only ever increases for whichever class gets chosen, so picking among anything but the
	 * shallowest available option here compounds every subsequent fallback pick on an already-too-deep
	 * branch. The attachment strategy still decides which of those tied-for-shallowest candidates gets used,
	 * so Chain/Balanced Tree/Preferential remain distinguishable even in fallback.
	 */
	private OWLClass chooseShallowest(GenerationContext ctx, List<OWLClass> safeFallback) {
		int minDepth = safeFallback.stream().mapToInt(c -> depth.getOrDefault(c, 0)).min().orElse(0);
		List<OWLClass> shallowest = safeFallback.stream()
				.filter(c -> depth.getOrDefault(c, 0) == minDepth)
				.toList();
		return ctx.pools().classes().chooseAmong(shallowest, ctx.random());
	}

	/**
	 * True if {@code subclass} is already reachable by walking up from {@code candidate} through
	 * previously-assigned parents — i.e. {@code candidate} is already (transitively) a subclass of
	 * {@code subclass}, so asserting {@code subclass} SubClassOf {@code candidate} would close a cycle.
	 */
	private boolean wouldCreateCycle(OWLClass subclass, OWLClass candidate) {
		if (candidate.equals(subclass)) {
			return true;
		}
		Deque<OWLClass> toVisit = new ArrayDeque<>();
		Set<OWLClass> visited = new HashSet<>();
		toVisit.push(candidate);
		while (!toVisit.isEmpty()) {
			OWLClass current = toVisit.pop();
			if (current.equals(subclass)) {
				return true;
			}
			if (!visited.add(current)) {
				continue;
			}
			for (OWLClass parent : parentsOf.getOrDefault(current, List.of())) {
				toVisit.push(parent);
			}
		}
		return false;
	}
}
