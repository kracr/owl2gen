package com.owl2gendl.api.dto;

import com.owl2gendl.metrics.MetricsReport;
import com.owl2gendl.reasoning.VerificationResult;

public record VariantResultDto(
		String variantId,
		String status,
		Long axiomCount,
		String errorMessage,
		VerificationResult verification,
		MetricsReport metrics) {
}
