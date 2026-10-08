package com.owl2gendl.reasoning;

import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Extension point for plugging in a reasoner this project does not ship with: implement this interface (it
 * adds nothing beyond the standard OWL API {@link OWLReasonerFactory} — any conformant reasoner already has
 * a class like this), register it as a Spring {@code @Component}, and requests using
 * {@link ReasonerTier#CUSTOM} will route to it.
 *
 * <p>Example, wrapping a hypothetical reasoner whose OWL API factory is {@code com.example.MyReasonerFactory}:
 *
 * <pre>{@code
 * @Component
 * public class MyCustomReasonerFactory implements CustomReasonerFactory {
 *     private final com.example.MyReasonerFactory delegate = new com.example.MyReasonerFactory();
 *
 *     @Override
 *     public String getReasonerName() {
 *         return delegate.getReasonerName();
 *     }
 *
 *     @Override
 *     public OWLReasoner createReasoner(OWLOntology ontology) {
 *         return delegate.createReasoner(ontology);
 *     }
 *
 *     @Override
 *     public OWLReasoner createReasoner(OWLOntology ontology, OWLReasonerConfiguration config) {
 *         return delegate.createReasoner(ontology, config);
 *     }
 *
 *     @Override
 *     public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
 *         return delegate.createNonBufferingReasoner(ontology);
 *     }
 *
 *     @Override
 *     public OWLReasoner createNonBufferingReasoner(OWLOntology ontology, OWLReasonerConfiguration config) {
 *         return delegate.createNonBufferingReasoner(ontology, config);
 *     }
 * }
 * }</pre>
 *
 * <p>Add the reasoner's own Maven dependency to {@code pom.xml} alongside this class. If it bundles its own
 * copy of OWL API under a different Maven coordinate (rather than depending on this project's
 * {@code net.sourceforge.owlapi:owlapi-distribution}), check for a split-package conflict the way the
 * project's own HermiT integration had to (see the comment on {@code hermit.version} in {@code pom.xml})
 * before assuming it will work at runtime just because it compiles.
 *
 * <p>If no bean implementing this interface is registered, selecting {@link ReasonerTier#CUSTOM} fails with a
 * clear {@link IllegalStateException} rather than a confusing {@code NoSuchBeanDefinitionException}.
 */
public interface CustomReasonerFactory extends OWLReasonerFactory {
}
