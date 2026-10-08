package com.owl2gendl.orchestration;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.owl2gendl.api.dto.EntityCountsDto;
import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.catalog.EntityRequirement;
import com.owl2gendl.catalog.MinimumEntityRequirements;

/**
 * Fills in whichever entity counts the user left unset. Left blank, a field falls back to this project's
 * configured default (20/6/3/10 unless overridden in {@code application.yml}) - bumped up only if the
 * selected constructs structurally need more than that to produce anything at all (e.g. selecting only
 * {@code ALL_DISJOINT_OBJECT_PROPERTIES} with the default 6 object properties is fine, but a request that
 * left objectProperties unset while selecting it would still resolve to at least 3, not 0). Explicit,
 * user-provided values are never touched here - too-low explicit values are rejected earlier by
 * {@code RequestValidator}, not silently bumped.
 */
@Service
public class EntityCountsResolver {

	private final int defaultClasses;
	private final int defaultObjectProperties;
	private final int defaultDataProperties;
	private final int defaultIndividuals;

	public EntityCountsResolver(
			@Value("${owl2gendl.generation.default-classes:20}") int defaultClasses,
			@Value("${owl2gendl.generation.default-object-properties:6}") int defaultObjectProperties,
			@Value("${owl2gendl.generation.default-data-properties:3}") int defaultDataProperties,
			@Value("${owl2gendl.generation.default-individuals:10}") int defaultIndividuals) {
		this.defaultClasses = defaultClasses;
		this.defaultObjectProperties = defaultObjectProperties;
		this.defaultDataProperties = defaultDataProperties;
		this.defaultIndividuals = defaultIndividuals;
	}

	public EntityCountsDto resolve(EntityCountsDto raw, Set<ConstructId> selectedConstructs) {
		EntityRequirement required = MinimumEntityRequirements.forSelection(selectedConstructs);
		return new EntityCountsDto(
				resolveField(raw == null ? null : raw.classes(), defaultClasses, required.classes()),
				resolveField(raw == null ? null : raw.objectProperties(), defaultObjectProperties, required.objectProperties()),
				resolveField(raw == null ? null : raw.dataProperties(), defaultDataProperties, required.dataProperties()),
				resolveField(raw == null ? null : raw.individuals(), defaultIndividuals, required.individuals()));
	}

	private int resolveField(Integer provided, int base, int minimumRequired) {
		return provided != null ? provided : Math.max(base, minimumRequired);
	}
}
