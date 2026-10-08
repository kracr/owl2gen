package com.owl2gendl.api.dto;

import java.util.List;

public record GenerationJobDto(
		String jobId,
		String status,
		String createdAt,
		List<VariantResultDto> variants,
		String error) {
}
