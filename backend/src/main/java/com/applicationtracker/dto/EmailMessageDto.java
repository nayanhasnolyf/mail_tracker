package com.applicationtracker.dto;

import java.time.Instant;

/**
 * Standard DTO representing an email message for classification and application tracking.
 */
public record EmailMessageDto(
    String id,
    String subject,
    String from,
    String body,
    Instant receivedAt
) {
    public static EmailMessageDto of(String subject, String from, String body) {
        return new EmailMessageDto(null, subject, from, body, Instant.now());
    }

    public static EmailMessageDto of(String id, String subject, String from, String body) {
        return new EmailMessageDto(id, subject, from, body, Instant.now());
    }

    public static EmailMessageDto of(String id, String subject, String from, String body, Instant receivedAt) {
        return new EmailMessageDto(id, subject, from, body, receivedAt != null ? receivedAt : Instant.now());
    }
}
