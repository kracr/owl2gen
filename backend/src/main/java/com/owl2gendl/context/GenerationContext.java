package com.owl2gendl.context;

import java.util.Random;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import com.owl2gendl.constraint.ConstraintTracker;
import com.owl2gendl.pool.EntityPoolManager;
import com.owl2gendl.reasoning.TargetProfile;
import com.owl2gendl.topology.HierarchyStrategy;
import com.owl2gendl.topology.NestingStrategy;

/**
 * All state for a single generation run (one job, one variant), instantiated fresh per run and threaded
 * through every generator/strategy call. This replaces the old codebase's {@code public static} mutable
 * fields shared across the whole JVM (a real bug: two concurrent generation requests could corrupt each
 * other's state) — nothing here is shared between concurrent runs.
 */
public class GenerationContext {

	private final OWLOntologyManager ontologyManager;
	private final OWLOntology ontology;
	private final OWLDataFactory dataFactory;
	private final DefaultPrefixManager prefixManager;
	private final EntityPoolManager pools;
	private final Random random;
	private final HierarchyStrategy hierarchyStrategy;
	private final NestingStrategy nestingStrategy;
	private final ConstraintTracker constraintTracker;
	private final TargetProfile targetProfile;

	public GenerationContext(OWLOntologyManager ontologyManager, OWLOntology ontology, OWLDataFactory dataFactory,
			DefaultPrefixManager prefixManager, EntityPoolManager pools, Random random,
			HierarchyStrategy hierarchyStrategy, NestingStrategy nestingStrategy, ConstraintTracker constraintTracker,
			TargetProfile targetProfile) {
		this.ontologyManager = ontologyManager;
		this.ontology = ontology;
		this.dataFactory = dataFactory;
		this.prefixManager = prefixManager;
		this.pools = pools;
		this.random = random;
		this.hierarchyStrategy = hierarchyStrategy;
		this.nestingStrategy = nestingStrategy;
		this.constraintTracker = constraintTracker;
		this.targetProfile = targetProfile;
	}

	public OWLOntologyManager ontologyManager() {
		return ontologyManager;
	}

	public OWLOntology ontology() {
		return ontology;
	}

	public OWLDataFactory dataFactory() {
		return dataFactory;
	}

	public DefaultPrefixManager prefixManager() {
		return prefixManager;
	}

	public EntityPoolManager pools() {
		return pools;
	}

	public Random random() {
		return random;
	}

	public HierarchyStrategy hierarchyStrategy() {
		return hierarchyStrategy;
	}

	public NestingStrategy nestingStrategy() {
		return nestingStrategy;
	}

	public ConstraintTracker constraintTracker() {
		return constraintTracker;
	}

	public TargetProfile targetProfile() {
		return targetProfile;
	}
}
