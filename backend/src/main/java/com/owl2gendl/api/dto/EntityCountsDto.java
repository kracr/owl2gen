package com.owl2gendl.api.dto;

import jakarta.validation.constraints.Min;

/**
 * Each field is nullable: a request may leave any or all of them unset, in which case
 * {@code EntityCountsResolver} fills them in (falling back to this project's defaults, bumped up only if the
 * selected constructs structurally need more). By the time this DTO reaches {@code EntityProvisioningService}
 * it is always fully resolved - no null fields survive past request handling.
 */
public record EntityCountsDto(
		@Min(0) Integer classes,
		@Min(0) Integer objectProperties,
		@Min(0) Integer dataProperties,
		@Min(0) Integer individuals) {
}
