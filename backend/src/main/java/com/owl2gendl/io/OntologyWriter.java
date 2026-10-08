package com.owl2gendl.io;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.springframework.stereotype.Component;

@Component
public class OntologyWriter {

	public enum Format {
		RDFXML, TURTLE, OWLXML, MANCHESTER, FUNCTIONAL
	}

	public String write(OWLOntology ontology, Format format) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ontology.getOWLOntologyManager().saveOntology(ontology, toDocumentFormat(format), out);
			return out.toString(java.nio.charset.StandardCharsets.UTF_8);
		} catch (OWLOntologyStorageException e) {
			throw new IllegalStateException("Failed to serialize ontology as " + format, e);
		}
	}

	public String contentType(Format format) {
		return switch (format) {
			case RDFXML, OWLXML -> "application/rdf+xml";
			case TURTLE -> "text/turtle";
			case MANCHESTER, FUNCTIONAL -> "text/plain";
		};
	}

	public String fileExtension(Format format) {
		return switch (format) {
			case RDFXML -> "rdf";
			case OWLXML -> "owx";
			case TURTLE -> "ttl";
			case MANCHESTER -> "omn";
			case FUNCTIONAL -> "ofn";
		};
	}

	private OWLDocumentFormat toDocumentFormat(Format format) {
		return switch (format) {
			case RDFXML -> new RDFXMLDocumentFormat();
			case TURTLE -> new TurtleDocumentFormat();
			case OWLXML -> new OWLXMLDocumentFormat();
			case MANCHESTER -> new ManchesterSyntaxDocumentFormat();
			case FUNCTIONAL -> new FunctionalSyntaxDocumentFormat();
		};
	}

	public static Format parse(String value) {
		return Format.valueOf(value.toUpperCase(Locale.ROOT));
	}
}
