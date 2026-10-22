package com.applicationtracker.dto;

import com.applicationtracker.domain.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request payload for manually updating an application's status or notes.
 */
@Schema(description = "Payload for updating application status or notes")
public record UpdateApplicationRequest(
    @Schema(description = "New application status (optional)", example = "INTERVIEW")
    ApplicationStatus status,

    @Schema(description = "Personal notes or feedback (optional, max 5000 chars)", example = "Completed initial screen. Team was very friendly.")
    @Size(max = 5000, message = "Notes cannot exceed 5000 characters")
    String notes
) {}
