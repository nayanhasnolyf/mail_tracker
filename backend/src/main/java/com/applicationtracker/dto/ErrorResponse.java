package com.applicationtracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Standard error response envelope returned on client and server errors.
 */
@Schema(description = "Standard API error response structure")
public record ErrorResponse(
    @Schema(description = "Timestamp of the error", example = "2026-10-23T01:30:00Z")
    Instant timestamp,

    @Schema(description = "HTTP status code", example = "404")
    int status,

    @Schema(description = "Short error category name", example = "Not Found")
    String error,

    @Schema(description = "Descriptive error message", example = "Application not found with id: ...")
    String message,

    @Schema(description = "Target API endpoint path where error occurred", example = "/api/applications/123")
    String path,

    @Schema(description = "Optional list of field-level validation errors (for 400 Bad Request)")
    List<ValidationErrorItem> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, String path, List<ValidationErrorItem> validationErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, validationErrors);
    }
}
