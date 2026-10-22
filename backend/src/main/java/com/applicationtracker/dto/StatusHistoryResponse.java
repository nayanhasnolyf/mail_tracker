package com.applicationtracker.dto;

import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.StatusHistory;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing an entry in an application's status history.
 */
public record StatusHistoryResponse(
    UUID id,
    ApplicationStatus status,
    Instant changedAt,
    String sourceEmailId
) {
    public static StatusHistoryResponse from(StatusHistory history) {
        if (history == null) return null;
        return new StatusHistoryResponse(
            history.getId(),
            history.getStatus(),
            history.getChangedAt(),
            history.getSourceEmailId()
        );
    }
}
