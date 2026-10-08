package com.owl2gendl.context;

import java.util.Random;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.springframework.stereotype.Component;

import com.owl2gendl.constraint.ConstraintRuleRegistry;
import com.owl2gendl.constraint.ConstraintTracker;
import com.owl2gendl.pool.EntityPoolManager;
import com.owl2gendl.reasoning.TargetProfile;
import com.owl2gendl.topology.AttachmentStrategy;
import com.owl2gendl.topology.DepthBiasedHierarchyStrategy;
import com.owl2gendl.topology.HierarchyStrategy;
import com.owl2gendl.topology.NestingStrategy;
import com.owl2gendl.topology.ProbabilisticNestingStrategy;
import com.owl2gendl.topology.TopologyConfig;
import com.owl2gendl.topology.VariantStrategyFactory;

@Component
public class GenerationContextFactory {

	private static final String BASE_IRI = "http://owl2gendl.com/generated#";

	private final ConstraintRuleRegistry constraintRuleRegistry;

	public GenerationContextFactory(ConstraintRuleRegistry constraintRuleRegistry) {
		this.constraintRuleRegistry = constraintRuleRegistry;
	}

	public GenerationContext create(Long seed) {
		return create(seed, TopologyConfig.defaults(), TargetProfile.DL);
	}

	public GenerationContext create(Long seed, TopologyConfig config) {
		return create(seed, config, TargetProfile.DL);
	}

	public GenerationContext create(Long seed, TopologyConfig config, TargetProfile targetProfile) {
		try {
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = manager.createOntology(IRI.create(BASE_IRI));
			OWLDataFactory factory = manager.getOWLDataFactory();
			DefaultPrefixManager prefixManager = new DefaultPrefixManager(BASE_IRI);
			EntityPoolManager pools = new EntityPoolManager(factory, prefixManager);

			AttachmentStrategy attachmentStrategy = VariantStrategyFactory.attachmentStrategyFor(config.variant());
			pools.applyAttachmentStrategy(attachmentStrategy);

			Random random = seed != null ? new Random(seed) : new Random();
			HierarchyStrategy hierarchyStrategy = new DepthBiasedHierarchyStrategy(
					config.hierarchyTargetDepth(), config.hierarchyBranchingFactor());
			NestingStrategy nestingStrategy = new ProbabilisticNestingStrategy(
					config.nestingMaxDepth(), config.nestingProbability(), targetProfile);
			ConstraintTracker constraintTracker = new ConstraintTracker(constraintRuleRegistry.rules());

			return new GenerationContext(manager, ontology, factory, prefixManager, pools, random,
					hierarchyStrategy, nestingStrategy, constraintTracker, targetProfile);
		} catch (OWLOntologyCreationException e) {
			throw new IllegalStateException("Failed to create a blank ontology for generation", e);
		}
	}
}
