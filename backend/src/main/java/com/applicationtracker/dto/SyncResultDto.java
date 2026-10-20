package com.applicationtracker.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Result returned after performing a Gmail sync operation.
 */
public record SyncResultDto(
    UUID userId,
    String status,
    boolean isFirstSync,
    int messagesFound,
    int messagesProcessed,
    String lastHistoryId,
    Instant syncedAt
) {}
