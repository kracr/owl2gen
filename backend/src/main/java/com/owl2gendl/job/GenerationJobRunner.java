package com.owl2gendl.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.owl2gendl.orchestration.VariantOrchestrator;
import com.owl2gendl.topology.TopologyVariant;

@Component
public class GenerationJobRunner {

	private static final Logger log = LoggerFactory.getLogger(GenerationJobRunner.class);

	private final VariantOrchestrator variantOrchestrator;

	public GenerationJobRunner(VariantOrchestrator variantOrchestrator) {
		this.variantOrchestrator = variantOrchestrator;
	}

	@Async
	public void run(GenerationJob job) {
		job.markRunning();
		try {
			for (TopologyVariant variant : job.requestedVariants()) {
				VariantOutcome outcome = job.outcome(variant);
				try {
					variantOrchestrator.run(job.id(), variant, job.request(), outcome);
				} catch (Throwable t) {
					// Catching Throwable, not just Exception, is deliberate: a NoClassDefFoundError from a
					// transitive dependency conflict (an Error, not an Exception - confirmed the hard way,
					// where this used to catch(Exception) and silently left the job stuck at RUNNING
					// forever with no failure ever recorded) must still mark the variant failed rather than
					// disappear past this boundary.
					log.warn("Variant {} failed for job {}", variant, job.id(), t);
					outcome.markFailed(String.valueOf(t.getMessage()));
				}
			}
			job.markCompleted();
		} catch (Throwable t) {
			log.error("Job {} failed", job.id(), t);
			job.markFailed(String.valueOf(t.getMessage()));
		}
	}
}
