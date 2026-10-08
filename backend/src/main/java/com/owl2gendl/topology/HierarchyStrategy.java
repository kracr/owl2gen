package com.owl2gendl.topology;

import org.semanticweb.owlapi.model.OWLClass;

import com.owl2gendl.context.GenerationContext;

/**
 * Chooses a superclass for a new {@code SubClassOf} axiom, shaping the class hierarchy independently of
 * which/how-many constructs were requested. This is the direct answer to the OWL2Bench-style limitation
 * where only ABox size is scalable and TBox shape is fixed — here hierarchy depth and branching factor are
 * first-class, user-tunable parameters.
 */
public interface HierarchyStrategy {

	OWLClass chooseSuperclass(GenerationContext ctx, OWLClass subclass);
}
