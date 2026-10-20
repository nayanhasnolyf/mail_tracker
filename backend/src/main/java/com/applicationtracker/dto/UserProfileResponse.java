package com.applicationtracker.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
    UUID id,
    String email,
    String googleSubjectId,
    Instant createdAt,
    boolean reauthRequired,
    boolean hasRefreshToken
) {}
