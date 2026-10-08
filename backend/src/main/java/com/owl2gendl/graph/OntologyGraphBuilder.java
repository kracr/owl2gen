package com.owl2gendl.graph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.springframework.stereotype.Component;

import com.owl2gendl.api.dto.GraphEdgeDto;
import com.owl2gendl.api.dto.GraphNodeDto;
import com.owl2gendl.api.dto.OntologyGraphDto;

/**
 * Builds a labeled, typed graph of an ontology's entities and their relationships, for the frontend's
 * visualization view. Deliberately separate from {@code metrics.EntityRelationGraphBuilder}, which is an
 * untyped, unlabeled adjacency used only for structural metrics (degree, clustering) — that one is tested
 * and load-bearing for numbers users see, so it's left untouched. This one keeps entity kind and axiom-type
 * labels, which a visualization needs but a structural metric doesn't, at the cost of being a best-effort
 * mapping (some axiom shapes fall back to a generic "co-occurred in this axiom" edge) rather than a
 * formally-defined measure.
 */
@Component
public class OntologyGraphBuilder {

	private static final int MAX_EDGES = 1500;

	public OntologyGraphDto build(OWLOntology ontology) {
		Map<String, GraphNodeDto> nodes = new LinkedHashMap<>();
		for (OWLEntity entity : ontology.getSignature()) {
			String id = nodeId(entity);
			nodes.put(id, new GraphNodeDto(id, shortForm(entity), entityType(entity)));
		}

		Map<String, GraphEdgeDto> edges = new LinkedHashMap<>();
		boolean truncated = false;
		outer:
		for (OWLAxiom axiom : ontology.getAxioms()) {
			if (axiom instanceof OWLDeclarationAxiom || axiom instanceof OWLAnnotationAssertionAxiom) {
				continue;
			}
			String label = axiom.getAxiomType().getName();
			for (String[] pair : directedPairs(axiom)) {
				addEdge(edges, pair[0], pair[1], label);
				if (edges.size() >= MAX_EDGES) {
					truncated = true;
					break outer;
				}
			}
		}

		return new OntologyGraphDto(new ArrayList<>(nodes.values()), new ArrayList<>(edges.values()), truncated);
	}

	/** [sourceId, targetId] pairs this axiom should draw edges for. Common binary axiom types get precise,
	 *  directional pairs; everything else falls back to connecting every pair of named entities that
	 *  co-occur in the axiom (same idea as EntityRelationGraphBuilder, just directionless in that case). */
	private List<String[]> directedPairs(OWLAxiom axiom) {
		List<String[]> pairs = new ArrayList<>();
		if (axiom instanceof OWLSubClassOfAxiom a && !a.getSubClass().isAnonymous() && !a.getSuperClass().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getSubClass().asOWLClass()), nodeId(a.getSuperClass().asOWLClass()) });
		} else if (axiom instanceof OWLClassAssertionAxiom a && !a.getIndividual().isAnonymous() && !a.getClassExpression().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getIndividual().asOWLNamedIndividual()), nodeId(a.getClassExpression().asOWLClass()) });
		} else if (axiom instanceof OWLObjectPropertyAssertionAxiom a && !a.getSubject().isAnonymous() && !a.getObject().isAnonymous()
				&& !a.getProperty().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getSubject().asOWLNamedIndividual()), nodeId(a.getObject().asOWLNamedIndividual()) });
		} else if (axiom instanceof OWLObjectPropertyDomainAxiom a && !a.getProperty().isAnonymous() && !a.getDomain().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getProperty().asOWLObjectProperty()), nodeId(a.getDomain().asOWLClass()) });
		} else if (axiom instanceof OWLObjectPropertyRangeAxiom a && !a.getProperty().isAnonymous() && !a.getRange().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getProperty().asOWLObjectProperty()), nodeId(a.getRange().asOWLClass()) });
		} else if (axiom instanceof OWLSubObjectPropertyOfAxiom a && !a.getSubProperty().isAnonymous() && !a.getSuperProperty().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getSubProperty().asOWLObjectProperty()), nodeId(a.getSuperProperty().asOWLObjectProperty()) });
		} else if (axiom instanceof OWLDataPropertyDomainAxiom a && !a.getDomain().isAnonymous()) {
			pairs.add(new String[] { nodeId(a.getProperty().asOWLDataProperty()), nodeId(a.getDomain().asOWLClass()) });
		} else if (axiom instanceof OWLSubDataPropertyOfAxiom a) {
			pairs.add(new String[] { nodeId(a.getSubProperty().asOWLDataProperty()), nodeId(a.getSuperProperty().asOWLDataProperty()) });
		} else {
			List<OWLEntity> entities = axiom.getSignature().stream()
					.filter(e -> e.isOWLClass() || e.isOWLObjectProperty() || e.isOWLDataProperty() || e.isOWLNamedIndividual())
					.toList();
			for (int i = 0; i < entities.size(); i++) {
				for (int j = i + 1; j < entities.size(); j++) {
					pairs.add(new String[] { nodeId(entities.get(i)), nodeId(entities.get(j)) });
				}
			}
		}
		return pairs;
	}

	private void addEdge(Map<String, GraphEdgeDto> edges, String source, String target, String label) {
		if (source.equals(target)) {
			return;
		}
		String key = source + "|" + target + "|" + label;
		edges.putIfAbsent(key, new GraphEdgeDto(key, source, target, label));
	}

	private String nodeId(OWLEntity entity) {
		return entity.getIRI().toString();
	}

	private String shortForm(OWLEntity entity) {
		return entity.getIRI().getShortForm();
	}

	private String entityType(OWLEntity entity) {
		if (entity.isOWLClass()) {
			return "CLASS";
		}
		if (entity.isOWLObjectProperty()) {
			return "OBJECT_PROPERTY";
		}
		if (entity.isOWLDataProperty()) {
			return "DATA_PROPERTY";
		}
		if (entity.isOWLNamedIndividual()) {
			return "INDIVIDUAL";
		}
		if (entity.isOWLAnnotationProperty()) {
			return "ANNOTATION_PROPERTY";
		}
		if (entity.isOWLDatatype()) {
			return "DATATYPE";
		}
		return "OTHER";
	}
}
