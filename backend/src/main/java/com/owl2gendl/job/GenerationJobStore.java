package com.owl2gendl.job;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.owl2gendl.api.dto.GenerationRequestDto;
import com.owl2gendl.topology.TopologyVariant;

/**
 * In-memory job store with TTL eviction — appropriate for a single-instance internal benchmarking tool;
 * would need to move to a database if multi-instance deployment or job history across restarts is ever
 * required.
 */
@Component
public class GenerationJobStore {

	private static final Duration TTL = Duration.ofMinutes(30);

	private final ConcurrentHashMap<UUID, GenerationJob> jobs = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, Instant> lastAccessed = new ConcurrentHashMap<>();

	public GenerationJob create(GenerationRequestDto request, List<TopologyVariant> variants) {
		GenerationJob job = new GenerationJob(UUID.randomUUID(), request, variants);
		jobs.put(job.id(), job);
		lastAccessed.put(job.id(), Instant.now());
		return job;
	}

	public Optional<GenerationJob> find(UUID id) {
		GenerationJob job = jobs.get(id);
		if (job != null) {
			lastAccessed.put(id, Instant.now());
		}
		return Optional.ofNullable(job);
	}

	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void evictExpired() {
		Instant cutoff = Instant.now().minus(TTL);
		lastAccessed.entrySet().removeIf(entry -> {
			boolean expired = entry.getValue().isBefore(cutoff);
			if (expired) {
				jobs.remove(entry.getKey());
			}
			return expired;
		});
	}
}
