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
import java.util.UUID;

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

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.applicationtracker.dto.ApplicationResponse> getApplications(
        User user,
        ApplicationStatus status,
        String companySearch,
        org.springframework.data.domain.Pageable pageable
    ) {
        String search = (companySearch != null && !companySearch.isBlank()) ? companySearch.trim() : null;
        return applicationRepository.findByFilters(user.getId(), status, search, pageable)
            .map(com.applicationtracker.dto.ApplicationResponse::from);
    }

    @Transactional(readOnly = true)
    public com.applicationtracker.dto.ApplicationDetailResponse getApplication(User user, java.util.UUID id) {
        Application app = applicationRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new com.applicationtracker.exception.ResourceNotFoundException("Application not found with id: " + id));
        java.util.List<StatusHistory> history = statusHistoryRepository.findByApplicationIdOrderByChangedAtDesc(app.getId());
        return com.applicationtracker.dto.ApplicationDetailResponse.from(app, history);
    }

    @Transactional
    public com.applicationtracker.dto.ApplicationDetailResponse updateApplication(
        User user,
        java.util.UUID id,
        com.applicationtracker.dto.UpdateApplicationRequest request
    ) {
        Application app = applicationRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new com.applicationtracker.exception.ResourceNotFoundException("Application not found with id: " + id));

        if (request.notes() != null) {
            app.setNotes(request.notes());
        }

        if (request.status() != null) {
            if (request.status() == ApplicationStatus.UNKNOWN) {
                throw new IllegalArgumentException("Cannot update application status to UNKNOWN");
            }

            if (app.getCurrentStatus() != request.status()) {
                app.setCurrentStatus(request.status());
                app.setLastUpdatedAt(Instant.now());

                StatusHistory history = StatusHistory.builder()
                    .application(app)
                    .status(request.status())
                    .changedAt(Instant.now())
                    .sourceEmailId(null)
                    .build();
                statusHistoryRepository.save(history);
            }
        }

        Application saved = applicationRepository.save(app);
        java.util.List<StatusHistory> history = statusHistoryRepository.findByApplicationIdOrderByChangedAtDesc(saved.getId());
        return com.applicationtracker.dto.ApplicationDetailResponse.from(saved, history);
    }

    @Transactional(readOnly = true)
    public com.applicationtracker.dto.ApplicationStatsResponse getStats(User user) {
        java.util.List<Application> apps = applicationRepository.findByUserId(user.getId());
        long total = apps.size();

        java.util.Map<ApplicationStatus, Long> countsByStatus = new java.util.EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) {
            if (s != ApplicationStatus.UNKNOWN) {
                countsByStatus.put(s, 0L);
            }
        }

        long activeCount = 0;
        long respondedCount = 0;

        java.util.List<StatusHistory> userHistories = statusHistoryRepository.findByUserIdOrderByChangedAtAsc(user.getId());
        java.util.Map<UUID, Instant> firstResponseByAppId = new java.util.HashMap<>();
        if (userHistories != null) {
            for (StatusHistory sh : userHistories) {
                ApplicationStatus st = sh.getStatus();
                if (st != ApplicationStatus.APPLIED && st != ApplicationStatus.UNKNOWN && sh.getApplication() != null) {
                    firstResponseByAppId.putIfAbsent(sh.getApplication().getId(), sh.getChangedAt());
                }
            }
        }

        double totalDaysToFirstResponse = 0.0;
        int appsWithResponseDuration = 0;

        java.util.Map<String, WeeklyAccumulator> weeklyMap = new java.util.TreeMap<>();
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;

        for (Application app : apps) {
            ApplicationStatus status = app.getCurrentStatus();
            if (status != null && status != ApplicationStatus.UNKNOWN) {
                countsByStatus.put(status, countsByStatus.getOrDefault(status, 0L) + 1);
            }

            if (status == ApplicationStatus.APPLIED || status == ApplicationStatus.ASSESSMENT || status == ApplicationStatus.INTERVIEW) {
                activeCount++;
            }

            if (status == ApplicationStatus.ASSESSMENT || status == ApplicationStatus.INTERVIEW
                || status == ApplicationStatus.OFFER || status == ApplicationStatus.REJECTED) {
                respondedCount++;
            }

            Instant responseTime = firstResponseByAppId.get(app.getId());
            if (responseTime == null) {
                if (status == ApplicationStatus.ASSESSMENT || status == ApplicationStatus.INTERVIEW
                    || status == ApplicationStatus.OFFER || status == ApplicationStatus.REJECTED) {
                    responseTime = app.getLastUpdatedAt();
                }
            }

            if (responseTime != null && app.getFirstSeenAt() != null) {
                long millis = java.time.Duration.between(app.getFirstSeenAt(), responseTime).toMillis();
                double days = Math.max(0.0, millis / (1000.0 * 60.0 * 60.0 * 24.0));
                totalDaysToFirstResponse += days;
                appsWithResponseDuration++;
            }

            Instant seenAt = app.getFirstSeenAt() != null ? app.getFirstSeenAt() : Instant.now();
            java.time.LocalDate localDate = seenAt.atZone(java.time.ZoneOffset.UTC).toLocalDate();
            java.time.LocalDate monday = localDate.with(weekFields.dayOfWeek(), 1);
            int year = localDate.get(weekFields.weekBasedYear());
            int week = localDate.get(weekFields.weekOfWeekBasedYear());
            String weekKey = String.format("%d-W%02d", year, week);

            weeklyMap.computeIfAbsent(weekKey, k -> new WeeklyAccumulator(k, monday.toString())).increment();
        }

        double responseRate = total == 0 ? 0.0 : Math.round((respondedCount * 1000.0) / total) / 10.0;
        Double averageDaysToFirstResponse = appsWithResponseDuration == 0
            ? 0.0
            : Math.round((totalDaysToFirstResponse / appsWithResponseDuration) * 10.0) / 10.0;

        java.util.List<com.applicationtracker.dto.WeeklyApplicationStatDto> weeklyStats = weeklyMap.values().stream()
            .map(acc -> new com.applicationtracker.dto.WeeklyApplicationStatDto(acc.weekKey, acc.startDate, acc.count))
            .toList();

        return new com.applicationtracker.dto.ApplicationStatsResponse(
            total,
            activeCount,
            responseRate,
            averageDaysToFirstResponse,
            countsByStatus,
            weeklyStats
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<com.applicationtracker.dto.ApplicationResponse> getPendingFollowups(User user, int days) {
        int thresholdDays = Math.max(1, days);
        Instant threshold = Instant.now().minus(thresholdDays, java.time.temporal.ChronoUnit.DAYS);
        return applicationRepository.findPendingFollowups(user.getId(), threshold).stream()
            .map(com.applicationtracker.dto.ApplicationResponse::from)
            .toList();
    }

    private static class WeeklyAccumulator {
        final String weekKey;
        final String startDate;
        long count;

        WeeklyAccumulator(String weekKey, String startDate) {
            this.weekKey = weekKey;
            this.startDate = startDate;
            this.count = 0;
        }

        void increment() {
            this.count++;
        }
    }
}
