package com.owl2gendl.api.dto;

import java.util.List;

public record CategoryDto(
		String id,
		String displayName,
		List<ConstructDescriptorDto> constructs) {
}
