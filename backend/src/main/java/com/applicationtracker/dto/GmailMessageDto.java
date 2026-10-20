package com.applicationtracker.dto;

import java.math.BigInteger;
import java.time.Instant;

/**
 * Parsed Gmail message containing extracted metadata and plain-text body.
 */
public record GmailMessageDto(
    String id,
    String threadId,
    BigInteger historyId,
    String subject,
    String from,
    String to,
    String snippet,
    String plainTextBody,
    Instant internalDate
) {}
