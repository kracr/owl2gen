package com.owl2gendl.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.springframework.stereotype.Component;

/**
 * Measures the shape of the generated class hierarchy: achieved depth, average branching factor, and
 * tangledness (fraction of classes with more than one direct superclass) — the direct measurement of what
 * the topology layer's {@code HierarchyStrategy} controls.
 */
@Component
public class HierarchyAnalyzer {

	public HierarchyMetrics analyze(OWLOntology ontology) {
		Map<OWLClass, Set<OWLClass>> parentsOf = new HashMap<>();
		for (OWLSubClassOfAxiom axiom : ontology.getAxioms(AxiomType.SUBCLASS_OF)) {
			if (axiom.getSubClass().isOWLClass() && axiom.getSuperClass().isOWLClass()) {
				parentsOf.computeIfAbsent(axiom.getSubClass().asOWLClass(), key -> new HashSet<>())
						.add(axiom.getSuperClass().asOWLClass());
			}
		}
		if (parentsOf.isEmpty()) {
			return new HierarchyMetrics(0, 0.0, 0.0);
		}

		Map<OWLClass, Integer> depthCache = new HashMap<>();
		int maxDepth = 0;
		for (OWLClass owlClass : parentsOf.keySet()) {
			maxDepth = Math.max(maxDepth, depthOf(owlClass, parentsOf, depthCache, new HashSet<>()));
		}

		Map<OWLClass, Integer> childCount = new HashMap<>();
		for (Set<OWLClass> parents : parentsOf.values()) {
			for (OWLClass parent : parents) {
				childCount.merge(parent, 1, Integer::sum);
			}
		}
		double avgBranchingFactor = childCount.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);

		long tangledCount = parentsOf.values().stream().filter(parents -> parents.size() > 1).count();
		double tangledness = (double) tangledCount / parentsOf.size();

		return new HierarchyMetrics(maxDepth, avgBranchingFactor, tangledness);
	}

	private int depthOf(OWLClass owlClass, Map<OWLClass, Set<OWLClass>> parentsOf, Map<OWLClass, Integer> cache,
			Set<OWLClass> inProgress) {
		Integer cached = cache.get(owlClass);
		if (cached != null) {
			return cached;
		}
		Set<OWLClass> parents = parentsOf.get(owlClass);
		if (parents == null || parents.isEmpty() || !inProgress.add(owlClass)) {
			return 0; // root, or a cycle guard (shouldn't occur given how generation works)
		}
		int maxParentDepth = 0;
		for (OWLClass parent : parents) {
			maxParentDepth = Math.max(maxParentDepth, depthOf(parent, parentsOf, cache, inProgress));
		}
		inProgress.remove(owlClass);
		int depth = maxParentDepth + 1;
		cache.put(owlClass, depth);
		return depth;
	}
}
