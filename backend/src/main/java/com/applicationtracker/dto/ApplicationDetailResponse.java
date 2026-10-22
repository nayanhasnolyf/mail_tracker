package com.applicationtracker.dto;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.StatusHistory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed DTO representation of a job application including its full status history.
 */
public record ApplicationDetailResponse(
    UUID id,
    String company,
    String role,
    ApplicationStatus currentStatus,
    String notes,
    String sourceEmailId,
    Instant firstSeenAt,
    Instant lastUpdatedAt,
    List<StatusHistoryResponse> statusHistory
) {
    public static ApplicationDetailResponse from(Application app, List<StatusHistory> history) {
        if (app == null) return null;
        List<StatusHistoryResponse> historyList = history != null
            ? history.stream().map(StatusHistoryResponse::from).toList()
            : List.of();

        return new ApplicationDetailResponse(
            app.getId(),
            app.getCompany(),
            app.getRole(),
            app.getCurrentStatus(),
            app.getNotes(),
            app.getSourceEmailId(),
            app.getFirstSeenAt(),
            app.getLastUpdatedAt(),
            historyList
        );
    }
}
