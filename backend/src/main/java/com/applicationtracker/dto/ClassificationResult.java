package com.applicationtracker.dto;

import com.applicationtracker.domain.ApplicationStatus;

/**
 * Result of classifying an email message, containing the determined application status,
 * extracted company name, extracted role, confidence score, and classification reasoning.
 */
public record ClassificationResult(
    ApplicationStatus status,
    String company,
    String role,
    double confidence,
    String reasoning
) {
    public static ClassificationResult unknown(String reasoning) {
        return new ClassificationResult(ApplicationStatus.UNKNOWN, null, null, 0.0, reasoning);
    }

    public static ClassificationResult of(
        ApplicationStatus status,
        String company,
        String role,
        double confidence,
        String reasoning
    ) {
        return new ClassificationResult(status, company, role, confidence, reasoning);
    }
}
