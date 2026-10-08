package com.owl2gendl.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.api.dto.GenerationRequestDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.catalog.EntityRequirement;
import com.owl2gendl.catalog.MinimumEntityRequirements;
import com.owl2gendl.reasoning.ReasonerTier;
import com.owl2gendl.reasoning.TargetProfile;
import com.owl2gendl.topology.TopologyVariant;

/**
 * Checks that Bean Validation annotations on the DTOs can't express: unknown construct/variant names (these
 * come from open-ended {@code String} fields, since JSON has no enum type) and a total-requested-axioms cap
 * so one request can't hand every reasoner run an unbounded amount of work. Runs before a job is created, so
 * a bad request fails fast with a 400 rather than being accepted and only failing per-variant during async
 * execution.
 */
@Component
public class RequestValidator {

	public static final int MAX_TOTAL_REQUESTED_AXIOMS = 1_500_000; // TEMP: raised for PyGraft-comparison large-scale test, revert to 5000 after

	public void validate(GenerationRequestDto request) {
		for (String name : request.constructs().keySet()) {
			if (!isValidConstructId(name)) {
				throw new InvalidRequestException("Unknown construct id: " + name);
			}
		}

		if (request.variants() != null) {
			for (String name : request.variants()) {
				if (!isValidTopologyVariant(name)) {
					throw new InvalidRequestException("Unknown topology variant: " + name);
				}
			}
		}

		int totalRequestedAxioms = request.constructs().values().stream().mapToInt(Integer::intValue).sum();
		if (totalRequestedAxioms > MAX_TOTAL_REQUESTED_AXIOMS) {
			throw new InvalidRequestException("Total requested axioms (%d) exceeds the maximum of %d per request"
					.formatted(totalRequestedAxioms, MAX_TOTAL_REQUESTED_AXIOMS));
		}

		TargetProfile profile;
		try {
			profile = TargetProfile.resolve(request.targetProfile());
		} catch (IllegalArgumentException e) {
			throw new InvalidRequestException("Unknown target profile: " + request.targetProfile());
		}
		if (profile == TargetProfile.EL) {
			var ineligible = request.constructs().keySet().stream()
					.filter(name -> !ConstructId.valueOf(name).elEligible())
					.toList();
			if (!ineligible.isEmpty()) {
				throw new InvalidRequestException(
						"The following selected constructs are not available in OWL 2 EL: " + ineligible);
			}
		}

		if (request.reasoning() != null && request.reasoning().tier() != null) {
			try {
				ReasonerTier.valueOf(request.reasoning().tier());
			} catch (IllegalArgumentException e) {
				throw new InvalidRequestException("Unknown reasoner tier: " + request.reasoning().tier());
			}
		}

		validateExplicitEntityCounts(request);
	}

	/**
	 * Entity counts are optional (see {@code EntityCountsResolver}) - a field left {@code null} is simply
	 * filled in later, never rejected here. But a field the user *did* set explicitly, too low for what
	 * they've selected, is rejected up front with a specific, actionable reason rather than silently minting
	 * extra entities behind their back.
	 */
	private void validateExplicitEntityCounts(GenerationRequestDto request) {
		EntityCountsDto counts = request.entityCounts();
		if (counts == null) {
			return;
		}
		Set<ConstructId> selected = request.constructs().keySet().stream().map(ConstructId::valueOf).collect(Collectors.toSet());
		List<String> problems = new ArrayList<>();
		checkMinimum(selected, EntityRequirement::classes, counts.classes(), "classes", problems);
		checkMinimum(selected, EntityRequirement::objectProperties, counts.objectProperties(), "object properties", problems);
		checkMinimum(selected, EntityRequirement::dataProperties, counts.dataProperties(), "data properties", problems);
		checkMinimum(selected, EntityRequirement::individuals, counts.individuals(), "individuals", problems);
		if (!problems.isEmpty()) {
			throw new InvalidRequestException("Not enough entities for the selected constructs: " + String.join("; ", problems)
					+ ". Leave the count blank (or raise it) to let the generator size the pool automatically.");
		}
	}

	private void checkMinimum(Set<ConstructId> selected, ToIntFunction<EntityRequirement> extractor, Integer provided,
			String label, List<String> problems) {
		if (provided == null) {
			return;
		}
		ConstructId offender = null;
		int required = 0;
		for (ConstructId id : selected) {
			int need = extractor.applyAsInt(MinimumEntityRequirements.forConstruct(id));
			if (need > required) {
				required = need;
				offender = id;
			}
		}
		if (provided < required) {
			problems.add(offender.displayName() + " needs at least " + required + " " + label + ", but you provided " + provided);
		}
	}

	private boolean isValidConstructId(String name) {
		try {
			ConstructId.valueOf(name);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private boolean isValidTopologyVariant(String name) {
		try {
			TopologyVariant.valueOf(name.toUpperCase());
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
