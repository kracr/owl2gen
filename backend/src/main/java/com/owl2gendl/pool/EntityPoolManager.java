package com.owl2gendl.pool;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.PrefixManager;

import com.owl2gendl.topology.AttachmentStrategy;

/** Bundles the four entity pools used during generation of a single ontology. */
public class EntityPoolManager {

	private final ClassPool classPool;
	private final ObjectPropertyPool objectPropertyPool;
	private final DataPropertyPool dataPropertyPool;
	private final IndividualPool individualPool;
	private final DatatypePool datatypePool;

	public EntityPoolManager(OWLDataFactory factory, PrefixManager prefixManager) {
		this.classPool = new ClassPool(factory, prefixManager);
		this.objectPropertyPool = new ObjectPropertyPool(factory, prefixManager);
		this.dataPropertyPool = new DataPropertyPool(factory, prefixManager);
		this.individualPool = new IndividualPool(factory, prefixManager);
		this.datatypePool = new DatatypePool(factory, prefixManager);
	}

	public ClassPool classes() {
		return classPool;
	}

	public ObjectPropertyPool objectProperties() {
		return objectPropertyPool;
	}

	public DataPropertyPool dataProperties() {
		return dataPropertyPool;
	}

	public IndividualPool individuals() {
		return individualPool;
	}

	public DatatypePool datatypes() {
		return datatypePool;
	}

	/** Applies one attachment strategy across every pool — the whole ontology's reuse pattern is one choice. */
	public void applyAttachmentStrategy(AttachmentStrategy strategy) {
		classPool.setAttachmentStrategy(strategy);
		objectPropertyPool.setAttachmentStrategy(strategy);
		dataPropertyPool.setAttachmentStrategy(strategy);
		individualPool.setAttachmentStrategy(strategy);
		datatypePool.setAttachmentStrategy(strategy);
	}
}
