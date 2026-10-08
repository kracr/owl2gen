package com.owl2gendl.metrics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.springframework.stereotype.Component;

/**
 * Builds an undirected adjacency-list graph connecting every entity pair that co-occurs in a non-declaration
 * axiom (subclass edges, restriction fillers, property assertions, disjointness/equivalence, ...) — a proxy
 * for "how interconnected is this ontology" independent of which specific constructs created each connection.
 *
 * <p>Plain Java collections rather than a graph library on purpose: {@code openllet-core} needs
 * {@code jgrapht-core:1.1.0} for its own taxonomy cycle detection, which predates the package reorganization
 * that added the modern algorithm classes (e.g. clustering coefficient) this module would otherwise want —
 * only one JGraphT version can be on the classpath, so pulling in a newer one breaks Openllet at runtime
 * (confirmed via a full end-to-end run). Degree and clustering coefficient are simple enough not to need a
 * library either way.
 */
@Component
public class EntityRelationGraphBuilder {

	public Map<OWLEntity, Set<OWLEntity>> build(OWLOntology ontology) {
		Map<OWLEntity, Set<OWLEntity>> adjacency = new HashMap<>();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			if (axiom instanceof OWLDeclarationAxiom) {
				continue;
			}
			List<OWLEntity> entities = new ArrayList<>(axiom.getSignature());
			entities.forEach(e -> adjacency.computeIfAbsent(e, key -> new HashSet<>()));
			for (int i = 0; i < entities.size(); i++) {
				for (int j = i + 1; j < entities.size(); j++) {
					OWLEntity a = entities.get(i);
					OWLEntity b = entities.get(j);
					if (!a.equals(b)) {
						adjacency.get(a).add(b);
						adjacency.get(b).add(a);
					}
				}
			}
		}
		return adjacency;
	}
}
