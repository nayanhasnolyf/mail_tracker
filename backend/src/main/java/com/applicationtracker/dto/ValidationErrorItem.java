package com.applicationtracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Details of a field-level validation failure.
 */
@Schema(description = "Validation error item")
public record ValidationErrorItem(
    @Schema(description = "Name of the invalid property", example = "notes")
    String field,

    @Schema(description = "Validation failure reason", example = "Notes cannot exceed 5000 characters")
    String message,

    @Schema(description = "Rejected value submitted by client")
    Object rejectedValue
) {}
