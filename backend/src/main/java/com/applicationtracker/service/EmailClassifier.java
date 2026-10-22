package com.applicationtracker.service;

import com.applicationtracker.dto.ClassificationResult;
import com.applicationtracker.dto.EmailMessageDto;

/**
 * Interface for classifying incoming job application emails and extracting metadata (company, role).
 */
public interface EmailClassifier {

    /**
     * Classifies an email message and extracts company name and role.
     *
     * @param email the email message to classify
     * @return the classification result containing status, company, role, confidence, and reasoning
     */
    ClassificationResult classify(EmailMessageDto email);

    /**
     * Convenience method to classify an email from raw subject, from header, and body text.
     *
     * @param subject email subject
     * @param from email from header
     * @param body email plain text body
     * @return the classification result
     */
    default ClassificationResult classify(String subject, String from, String body) {
        return classify(EmailMessageDto.of(subject, from, body));
    }
}
