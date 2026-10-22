package com.applicationtracker.service;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.StatusHistory;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.ClassificationResult;
import com.applicationtracker.dto.EmailMessageDto;
import com.applicationtracker.repository.ApplicationRepository;
import com.applicationtracker.repository.StatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Service managing job applications and status progression from classified emails.
 * Guarantees that application statuses only advance forward sensibly according
 * to defined lifecycle rules.
 */
@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final EmailClassifier emailClassifier;
    private final ApplicationRepository applicationRepository;
    private final StatusHistoryRepository statusHistoryRepository;

    public ApplicationService(
        EmailClassifier emailClassifier,
        ApplicationRepository applicationRepository,
        StatusHistoryRepository statusHistoryRepository
    ) {
        this.emailClassifier = emailClassifier;
        this.applicationRepository = applicationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    /**
     * Classifies an incoming email and creates or updates the corresponding Application and StatusHistory,
     * ensuring status only moves forward sensibly.
     *
     * @param user the user whose mailbox this email belongs to
     * @param email the incoming email message
     * @return the created or updated Application, or empty if email is UNKNOWN/non-actionable
     */
    @Transactional
    public Optional<Application> processEmail(User user, EmailMessageDto email) {
        if (user == null || email == null) {
            return Optional.empty();
        }

        ClassificationResult classification = emailClassifier.classify(email);
        return processClassification(user, email, classification);
    }

    /**
     * Creates or updates Application and StatusHistory from a classification result.
     *
     * @param user the user
     * @param email the source email
     * @param classification the classification result
     * @return the created or updated application, or empty if status is UNKNOWN
     */
    @Transactional
    public Optional<Application> processClassification(User user, EmailMessageDto email, ClassificationResult classification) {
        if (user == null || classification == null || !classification.status().isActionable()) {
            log.debug("Skipping non-actionable email classification (status={})",
                classification != null ? classification.status() : null);
            return Optional.empty();
        }

        Instant timestamp = email != null && email.receivedAt() != null ? email.receivedAt() : Instant.now();
        String emailId = email != null ? email.id() : null;

        // 1. Check if application was already created for this user and sourceEmailId
        if (emailId != null) {
            Optional<Application> existingByEmail = applicationRepository.findByUserIdAndSourceEmailId(user.getId(), emailId);
            if (existingByEmail.isPresent()) {
                log.debug("Application already exists with source email id {}", emailId);
                return existingByEmail;
            }
        }

        // 2. Find existing application for this user and company
        Optional<Application> existingAppOpt = findExistingApplication(user, classification.company(), classification.role());

        if (existingAppOpt.isEmpty()) {
            // Create brand new application
            Application newApplication = Application.builder()
                .user(user)
                .company(classification.company() != null ? classification.company() : "Unknown Company")
                .role(classification.role() != null ? classification.role() : "Role Not Specified")
                .currentStatus(classification.status())
                .sourceEmailId(emailId)
                .firstSeenAt(timestamp)
                .lastUpdatedAt(timestamp)
                .build();

            Application savedApp = applicationRepository.save(newApplication);

            // Record initial StatusHistory
            StatusHistory initialHistory = StatusHistory.builder()
                .application(savedApp)
                .status(classification.status())
                .changedAt(timestamp)
                .sourceEmailId(emailId)
                .build();
            statusHistoryRepository.save(initialHistory);

            log.info("Created new application id={} for company='{}' status={}",
                savedApp.getId(), savedApp.getCompany(), savedApp.getCurrentStatus());
            return Optional.of(savedApp);
        }

        // 3. Existing application found: check sensible state progression
        Application app = existingAppOpt.get();

        // Idempotency: check if this source email was already recorded in StatusHistory for this app
        if (emailId != null && statusHistoryRepository.existsByApplicationIdAndSourceEmailId(app.getId(), emailId)) {
            log.debug("Email {} already recorded in status history for application {}", emailId, app.getId());
            return Optional.of(app);
        }

        ApplicationStatus current = app.getCurrentStatus();
        ApplicationStatus next = classification.status();

        if (current.canTransitionTo(next)) {
            log.info("Advancing application {} status: {} -> {}", app.getId(), current, next);
            app.setCurrentStatus(next);
            app.setLastUpdatedAt(timestamp);

            // Backfill role if missing or generic
            if (("Role Not Specified".equalsIgnoreCase(app.getRole()) || app.getRole().isBlank())
                && classification.role() != null && !classification.role().isBlank()
                && !"Role Not Specified".equalsIgnoreCase(classification.role())) {
                app.setRole(classification.role());
            }

            Application savedApp = applicationRepository.save(app);

            StatusHistory history = StatusHistory.builder()
                .application(savedApp)
                .status(next)
                .changedAt(timestamp)
                .sourceEmailId(emailId)
                .build();
            statusHistoryRepository.save(history);

            return Optional.of(savedApp);
        } else {
            // Not a forward transition (duplicate status or backward transition)
            log.info("Ignored status transition for application {}: current={} candidate={} (not moving forward sensibly)",
                app.getId(), current, next);

            // If same status, refresh lastUpdatedAt without changing status
            if (current == next) {
                app.setLastUpdatedAt(timestamp);
                applicationRepository.save(app);
            }
            return Optional.of(app);
        }
    }

    private Optional<Application> findExistingApplication(User user, String company, String role) {
        if (company == null || company.isBlank() || "Unknown Company".equalsIgnoreCase(company)) {
            return Optional.empty();
        }

        // Try exact match on user, company, and role first
        if (role != null && !role.isBlank() && !"Role Not Specified".equalsIgnoreCase(role)) {
            Optional<Application> withRole = applicationRepository
                .findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(user.getId(), company, role);
            if (withRole.isPresent()) {
                return withRole;
            }
        }

        // Fall back to matching by company
        return applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseOrderByFirstSeenAtDesc(user.getId(), company);
    }
}
