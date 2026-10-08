package com.owl2gendl.api.dto;

public record ConstructDescriptorDto(
		String id,
		String displayName,
		String description,
		boolean supportsNesting,
		boolean requiresIndividuals,
		boolean elEligible) {
}
