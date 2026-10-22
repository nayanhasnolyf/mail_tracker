package com.applicationtracker.dto;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary DTO representation of a job application.
 */
public record ApplicationResponse(
    UUID id,
    String company,
    String role,
    ApplicationStatus currentStatus,
    String notes,
    String sourceEmailId,
    Instant firstSeenAt,
    Instant lastUpdatedAt
) {
    public static ApplicationResponse from(Application app) {
        if (app == null) return null;
        return new ApplicationResponse(
            app.getId(),
            app.getCompany(),
            app.getRole(),
            app.getCurrentStatus(),
            app.getNotes(),
            app.getSourceEmailId(),
            app.getFirstSeenAt(),
            app.getLastUpdatedAt()
        );
    }
}
