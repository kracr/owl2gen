package com.owl2gendl.reasoning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.owl2gendl.io.OntologyWriter;

/**
 * Runs Konclude's {@code consistency} CLI command as an external process, since Konclude has no OWL API
 * binding — {@link ReasonerTier#KONCLUDE} is the one tier {@link VerificationService} cannot create as an
 * {@link org.semanticweb.owlapi.reasoner.OWLReasoner}.
 *
 * <p>Killing a hung subprocess on timeout ({@link Process#destroyForcibly()}) is more reliable than
 * interrupting an in-JVM reasoner thread, which {@link VerificationService} already documents as
 * best-effort — a real advantage of this tier over the in-JVM ones for bounding worst-case latency.
 *
 * <p>Real, observed Konclude output ends with one of two lines once processing completes:
 * {@code "... is consistent."} or {@code "... is inconsistent."}; every other line is progress/info logging
 * that is not parsed, only kept for diagnostics.
 */
@Component
public class KoncludeConsistencyChecker {

	private static final Logger log = LoggerFactory.getLogger(KoncludeConsistencyChecker.class);

	private final OntologyWriter ontologyWriter;
	private final String executablePath;
	private final String workerThreads;

	public KoncludeConsistencyChecker(OntologyWriter ontologyWriter,
			@Value("${owl2gendl.reasoning.konclude-executable:}") String executablePath,
			@Value("${owl2gendl.reasoning.konclude-worker-threads:AUTO}") String workerThreads) {
		this.ontologyWriter = ontologyWriter;
		this.executablePath = executablePath;
		this.workerThreads = workerThreads;
	}

	public boolean isConfigured() {
		return executablePath != null && !executablePath.isBlank();
	}

	public VerificationStatus check(OWLOntology ontology, Duration timeout) {
		if (!isConfigured()) {
			throw new IllegalStateException(
					"ReasonerTier.KONCLUDE was selected but owl2gendl.reasoning.konclude-executable is not set");
		}

		Path tempFile;
		try {
			// OWL/XML, not RDF/XML: Konclude's own CLI examples ship test ontologies as .owl.xml, and its
			// format auto-detection is not reliable for RDF/XML - confirmed via a real, reproducible case
			// (a trivial two-axiom inconsistent ontology) where it logged "OWL2/XML Ontology node not found"
			// for an RDF/XML file, then silently reported the resulting empty parse as CONSISTENT rather than
			// surfacing the parse failure - a false negative for actual inconsistency, not a fallback. OWL/XML
			// parses this project's ontologies correctly and gives the right verdict.
			tempFile = Files.createTempFile("owl2gendl-konclude-", ".owx");
			Files.writeString(tempFile, ontologyWriter.write(ontology, OntologyWriter.Format.OWLXML), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.warn("Failed to write temp ontology file for Konclude", e);
			return VerificationStatus.NOT_VERIFIED_ERROR;
		}

		Process process = null;
		try {
			// -w enables Konclude's actual selling point (parallel reasoning). Omitting it entirely
			// defaults to something that behaves like single-threaded processing and took 300x+ longer on
			// an ontology otherwise solved in well under a second in direct testing - not a tuning nicety.
			// AUTO (all logical cores) is not necessarily the right choice for THIS call pattern, though:
			// strictConsistency invokes this once per construct type, potentially 100+ times per generation
			// request, so a fixed, more modest worker count trades peak single-call speed for lower
			// per-call thread-spinup overhead and less contention with whatever else is using the machine -
			// configurable rather than hardcoded, since the right value is workload- and deployment-specific.
			ProcessBuilder builder = new ProcessBuilder(
					executablePath, "consistency", "-w", workerThreads, "-i", tempFile.toAbsolutePath().toString());
			builder.redirectErrorStream(true);
			process = builder.start();

			// Konclude prints many progress lines; the output stream must be drained concurrently with
			// waitFor, or a full OS pipe buffer can deadlock the process before it ever reaches the timeout.
			List<String> output = new ArrayList<>();
			Process finalProcess = process;
			Thread drain = new Thread(() -> drainOutput(finalProcess, output), "konclude-output-drain");
			drain.setDaemon(true);
			drain.start();

			boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (!finished) {
				process.destroyForcibly();
				return VerificationStatus.NOT_VERIFIED_TIMEOUT;
			}
			drain.join(Duration.ofSeconds(2).toMillis());

			for (String line : output) {
				if (line.endsWith("is inconsistent.")) {
					return VerificationStatus.INCONSISTENT;
				}
				if (line.endsWith("is consistent.")) {
					return VerificationStatus.CONSISTENT;
				}
			}
			log.warn("Konclude produced no recognizable consistency verdict; output: {}", output);
			return VerificationStatus.NOT_VERIFIED_ERROR;
		} catch (IOException e) {
			log.warn("Failed to run Konclude", e);
			return VerificationStatus.NOT_VERIFIED_ERROR;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (process != null) {
				process.destroyForcibly();
			}
			return VerificationStatus.NOT_VERIFIED_ERROR;
		} finally {
			try {
				Files.deleteIfExists(tempFile);
			} catch (IOException ignored) {
				// Best-effort cleanup; a leftover temp file is harmless.
			}
		}
	}

	private void drainOutput(Process process, List<String> sink) {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				sink.add(line);
			}
		} catch (IOException ignored) {
			// Process was killed (timeout) or ended; nothing more to read.
		}
	}
}
