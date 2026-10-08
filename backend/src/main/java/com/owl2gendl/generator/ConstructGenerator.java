package com.owl2gendl.generator;

import java.util.List;

import org.semanticweb.owlapi.model.OWLAxiom;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.context.GenerationContext;

/**
 * Produces axioms for exactly one {@link ConstructId}. Implementations are Spring beans, auto-collected by
 * {@link ConstructGeneratorRegistry} — this replaces the old codebase's reflection-based dispatch
 * ({@code method.invoke(...)} against a name-keyed object map) with a typed, compile-time-checked registry.
 *
 * <p>Generators return axioms rather than mutating the ontology directly, so they stay pure and unit-testable
 * in isolation (see the M3 completeness test plan) — {@code OntologyGenerationService} is responsible for
 * adding the returned axioms to the ontology.
 */
public interface ConstructGenerator {

	ConstructId id();

	List<OWLAxiom> generate(GenerationContext ctx, int count);
}
