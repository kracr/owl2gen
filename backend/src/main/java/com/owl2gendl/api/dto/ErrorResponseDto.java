package com.owl2gendl.api.dto;

import java.util.List;

public record ErrorResponseDto(String message, List<String> details) {

	public ErrorResponseDto(String message) {
		this(message, List.of());
	}
}
