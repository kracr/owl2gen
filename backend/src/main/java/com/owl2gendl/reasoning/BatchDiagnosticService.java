package com.owl2gendl.reasoning;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.io.OntologyWriter;

/**
 * Persists a record of every "notable" per-construct-type batch check under {@code strictConsistency}: one
 * where the primary reasoner tier alone did not settle the question, whether or not the batch was ultimately
 * kept. This is the log the project's own reasoner-comparison evaluation is meant to mine — a case where
 * Openllet timed out but Konclude confirmed consistency quickly, or where every configured tier failed to
 * reach a verdict at all (previously silent: the batch was simply waved through with no record), is exactly
 * the kind of divergence worth reporting on, not just a user-facing debugging aid.
 *
 * <p>A batch is notable if more than one tier was attempted (the first one alone did not resolve it), if no
 * tier resolved it at all ({@link BatchCheckOutcome#resolved()} false), or if it was ultimately dropped after
 * every retry. A batch resolved cleanly by the first configured tier on the first attempt produces no record
 * at all — that is the ordinary case, not a diagnostic.
 */
@Service
public class BatchDiagnosticService {

	private static final Logger log = LoggerFactory.getLogger(BatchDiagnosticService.class);

	private final OntologyWriter ontologyWriter;
	private final Path diagnosticsRoot;

	public BatchDiagnosticService(OntologyWriter ontologyWriter,
			@Value("${owl2gendl.reasoning.diagnostics-directory:diagnostics}") String diagnosticsDirectory) {
		this.ontologyWriter = ontologyWriter;
		this.diagnosticsRoot = Path.of(diagnosticsDirectory);
	}

	public void recordIfNotable(UUID jobId, ConstructId constructId, int attemptNumber, boolean droppedFinal,
			BatchCheckOutcome outcome, OWLOntology ontology) {
		boolean notable = !outcome.resolved() || outcome.attempts().size() > 1 || droppedFinal;
		if (!notable) {
			return;
		}

		Map.Entry<ReasonerTier, VerificationStatus> last = null;
		for (Map.Entry<ReasonerTier, VerificationStatus> entry : outcome.attempts().entrySet()) {
			last = entry;
		}
		String tierLabel = last != null ? last.getKey().name() : "NONE";
		String statusLabel = last != null ? last.getValue().name() : "NONE";

		try {
			Path jobDir = diagnosticsRoot.resolve(jobId.toString());
			Files.createDirectories(jobDir);

			String baseName = "dropped_%s_attempt%d_%s_%s".formatted(constructId, attemptNumber, tierLabel, statusLabel);
			Path ontologyFile = jobDir.resolve(baseName + ".owl");
			Files.writeString(ontologyFile, ontologyWriter.write(ontology, OntologyWriter.Format.RDFXML), StandardCharsets.UTF_8);

			String reportLine = "{\"timestamp\":\"%s\",\"constructId\":\"%s\",\"attempt\":%d,\"droppedFinal\":%b,\"resolved\":%b,\"keepBatch\":%b,\"attempts\":%s,\"ontologyFile\":\"%s\"}%n"
					.formatted(Instant.now(), constructId, attemptNumber, droppedFinal, outcome.resolved(),
							outcome.keepBatch(), toJsonObject(outcome.attempts()), ontologyFile.getFileName());
			Files.writeString(jobDir.resolve("report.jsonl"), reportLine, StandardCharsets.UTF_8,
					java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		} catch (IOException e) {
			log.warn("Failed to write batch diagnostic for job {} construct {}", jobId, constructId, e);
		}
	}

	private String toJsonObject(Map<ReasonerTier, VerificationStatus> attempts) {
		StringBuilder sb = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<ReasonerTier, VerificationStatus> entry : attempts.entrySet()) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
		}
		return sb.append("}").toString();
	}
}
