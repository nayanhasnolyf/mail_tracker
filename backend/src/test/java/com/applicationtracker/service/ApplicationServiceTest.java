package com.applicationtracker.service;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.StatusHistory;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.ClassificationResult;
import com.applicationtracker.dto.EmailMessageDto;
import com.applicationtracker.repository.ApplicationRepository;
import com.applicationtracker.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private EmailClassifier emailClassifier;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    private ApplicationService applicationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(emailClassifier, applicationRepository, statusHistoryRepository);
        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .googleSubjectId("google-sub-123")
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("Create new Application and initial StatusHistory on APPLIED email")
    void testCreateNewApplicationOnAppliedEmail() {
        EmailMessageDto email = EmailMessageDto.of(
            "email-01",
            "Thank you for applying to Stripe",
            "recruiting@stripe.com",
            "We received your application for Software Engineer."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.APPLIED,
            "Stripe",
            "Software Engineer",
            0.95,
            "Matched application confirmation"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-01")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            eq(testUser.getId()), eq("Stripe"), eq("Software Engineer")
        )).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseOrderByFirstSeenAtDesc(
            eq(testUser.getId()), eq("Stripe")
        )).thenReturn(Optional.empty());

        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> {
            Application app = inv.getArgument(0);
            app.setId(UUID.randomUUID());
            return app;
        });

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        Application app = result.get();
        assertThat(app.getCompany()).isEqualTo("Stripe");
        assertThat(app.getRole()).isEqualTo("Software Engineer");
        assertThat(app.getCurrentStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(app.getSourceEmailId()).isEqualTo("email-01");

        ArgumentCaptor<StatusHistory> historyCaptor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        StatusHistory savedHistory = historyCaptor.getValue();
        assertThat(savedHistory.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(savedHistory.getSourceEmailId()).isEqualTo("email-01");
    }

    @Test
    @DisplayName("Sensibly advance status from APPLIED to ASSESSMENT")
    void testAdvanceStatusAppliedToAssessment() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Stripe")
            .role("Software Engineer")
            .currentStatus(ApplicationStatus.APPLIED)
            .sourceEmailId("email-01")
            .build();

        EmailMessageDto email = EmailMessageDto.of(
            "email-02",
            "Complete your Stripe HackerRank challenge",
            "assessments@hackerrank.com",
            "Please take the timed coding challenge."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.ASSESSMENT,
            "Stripe",
            "Software Engineer",
            0.90,
            "Matched coding challenge"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-02")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Stripe", "Software Engineer"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-02")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.ASSESSMENT);

        ArgumentCaptor<StatusHistory> historyCaptor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.ASSESSMENT);
        assertThat(historyCaptor.getValue().getSourceEmailId()).isEqualTo("email-02");
    }

    @Test
    @DisplayName("Sensibly advance status from ASSESSMENT to INTERVIEW")
    void testAdvanceStatusAssessmentToInterview() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Stripe")
            .role("Software Engineer")
            .currentStatus(ApplicationStatus.ASSESSMENT)
            .sourceEmailId("email-02")
            .build();

        EmailMessageDto email = EmailMessageDto.of(
            "email-03",
            "Invitation to Technical Interview with Stripe",
            "recruiting@stripe.com",
            "We would like to invite you to interview for the Software Engineer role."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.INTERVIEW,
            "Stripe",
            "Software Engineer",
            0.95,
            "Matched interview invitation"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-03")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Stripe", "Software Engineer"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-03")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.INTERVIEW);

        verify(statusHistoryRepository).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Sensibly advance status from INTERVIEW to OFFER")
    void testAdvanceStatusInterviewToOffer() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Stripe")
            .role("Software Engineer")
            .currentStatus(ApplicationStatus.INTERVIEW)
            .sourceEmailId("email-03")
            .build();

        EmailMessageDto email = EmailMessageDto.of(
            "email-04",
            "Offer of employment - Stripe",
            "recruiting@stripe.com",
            "We are pleased to offer you the position of Software Engineer at Stripe!"
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.OFFER,
            "Stripe",
            "Software Engineer",
            0.98,
            "Matched job offer"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-04")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Stripe", "Software Engineer"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-04")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.OFFER);

        ArgumentCaptor<StatusHistory> historyCaptor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getStatus()).isEqualTo(ApplicationStatus.OFFER);
    }

    @Test
    @DisplayName("Sensibly advance status from INTERVIEW to REJECTED")
    void testAdvanceStatusInterviewToRejected() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Google")
            .role("Backend Engineer")
            .currentStatus(ApplicationStatus.INTERVIEW)
            .sourceEmailId("email-10")
            .build();

        EmailMessageDto email = EmailMessageDto.of(
            "email-11",
            "Update on your application with Google",
            "careers@google.com",
            "Unfortunately, we have decided to move forward with other candidates."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.REJECTED,
            "Google",
            "Backend Engineer",
            0.95,
            "Matched rejection keywords"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-11")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Google", "Backend Engineer"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-11")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(statusHistoryRepository).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Prevent backward regression: REJECTED status does NOT revert to APPLIED")
    void testPreventBackwardRegressionRejectedToApplied() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Amazon")
            .role("Software Engineer")
            .currentStatus(ApplicationStatus.REJECTED)
            .sourceEmailId("email-rej")
            .build();

        EmailMessageDto outOfOrderEmail = EmailMessageDto.of(
            "email-old-app",
            "Application Received: Software Engineer",
            "jobs@amazon.com",
            "Thank you for applying to Amazon."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.APPLIED,
            "Amazon",
            "Software Engineer",
            0.90,
            "Matched application confirmation"
        );

        when(emailClassifier.classify(outOfOrderEmail)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-old-app")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Amazon", "Software Engineer"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-old-app")).thenReturn(false);

        Optional<Application> result = applicationService.processEmail(testUser, outOfOrderEmail);

        assertThat(result).isPresent();
        // Crucial: status must remain REJECTED, not regress to APPLIED!
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(statusHistoryRepository, never()).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Prevent backward regression: OFFER status does NOT revert to INTERVIEW")
    void testPreventBackwardRegressionOfferToInterview() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Apple")
            .role("iOS Engineer")
            .currentStatus(ApplicationStatus.OFFER)
            .sourceEmailId("email-off")
            .build();

        EmailMessageDto email = EmailMessageDto.of(
            "email-old-int",
            "Schedule interview",
            "recruiting@apple.com",
            "Invitation to interview."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.INTERVIEW,
            "Apple",
            "iOS Engineer",
            0.90,
            "Matched interview"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-old-int")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Apple", "iOS Engineer"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-old-int")).thenReturn(false);

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        // Crucial: status must remain OFFER!
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.OFFER);
        verify(statusHistoryRepository, never()).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("Idempotent handling: same email ID does NOT create duplicate status history")
    void testIdempotentSameEmailId() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Stripe")
            .role("Software Engineer")
            .currentStatus(ApplicationStatus.INTERVIEW)
            .sourceEmailId("email-01")
            .build();

        EmailMessageDto duplicateEmail = EmailMessageDto.of(
            "email-02",
            "Interview invitation",
            "recruiting@stripe.com",
            "Invitation to interview."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.INTERVIEW,
            "Stripe",
            "Software Engineer",
            0.90,
            "Matched interview"
        );

        when(emailClassifier.classify(duplicateEmail)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-02")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Stripe", "Software Engineer"
        )).thenReturn(Optional.of(existing));
        // Already recorded
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-02")).thenReturn(true);

        Optional<Application> result = applicationService.processEmail(testUser, duplicateEmail);

        assertThat(result).isPresent();
        verify(statusHistoryRepository, never()).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("UNKNOWN email is ignored and does NOT create or update applications")
    void testUnknownEmailIgnored() {
        EmailMessageDto spam = EmailMessageDto.of(
            "email-spam",
            "Your weekly newsletter",
            "newsletter@example.com",
            "Check out these articles! Unsubscribe here."
        );

        when(emailClassifier.classify(spam)).thenReturn(ClassificationResult.unknown("Noise detected"));

        Optional<Application> result = applicationService.processEmail(testUser, spam);

        assertThat(result).isEmpty();
        verifyNoInteractions(applicationRepository);
        verifyNoInteractions(statusHistoryRepository);
    }

    @Test
    @DisplayName("Backfill role when existing application had 'Role Not Specified'")
    void testBackfillRoleWhenMissing() {
        Application existing = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Netflix")
            .role("Role Not Specified")
            .currentStatus(ApplicationStatus.APPLIED)
            .sourceEmailId("email-01")
            .build();

        EmailMessageDto email = EmailMessageDto.of(
            "email-02",
            "Interview for Cloud Infrastructure Engineer at Netflix",
            "talent@netflix.com",
            "Invitation to interview for the Cloud Infrastructure Engineer role."
        );

        ClassificationResult classification = new ClassificationResult(
            ApplicationStatus.INTERVIEW,
            "Netflix",
            "Cloud Infrastructure Engineer",
            0.95,
            "Matched interview"
        );

        when(emailClassifier.classify(email)).thenReturn(classification);
        when(applicationRepository.findByUserIdAndSourceEmailId(testUser.getId(), "email-02")).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseAndRoleIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Netflix", "Cloud Infrastructure Engineer"
        )).thenReturn(Optional.empty());
        when(applicationRepository.findFirstByUserIdAndCompanyIgnoreCaseOrderByFirstSeenAtDesc(
            testUser.getId(), "Netflix"
        )).thenReturn(Optional.of(existing));
        when(statusHistoryRepository.existsByApplicationIdAndSourceEmailId(existing.getId(), "email-02")).thenReturn(false);
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Application> result = applicationService.processEmail(testUser, email);

        assertThat(result).isPresent();
        assertThat(result.get().getRole()).isEqualTo("Cloud Infrastructure Engineer");
        assertThat(result.get().getCurrentStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    @DisplayName("getApplications queries repository by filters and maps to Page<ApplicationResponse>")
    void testGetApplications() {
        UUID appId = UUID.randomUUID();
        Application app = Application.builder()
            .id(appId)
            .user(testUser)
            .company("Stripe")
            .role("Software Engineer")
            .currentStatus(ApplicationStatus.INTERVIEW)
            .notes("Great conversation")
            .sourceEmailId("email-1")
            .firstSeenAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .build();

        org.springframework.data.domain.Page<Application> page =
            new org.springframework.data.domain.PageImpl<>(java.util.List.of(app));

        when(applicationRepository.findByFilters(
            eq(testUser.getId()),
            eq(ApplicationStatus.INTERVIEW),
            eq("stripe"),
            any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(page);

        org.springframework.data.domain.Page<com.applicationtracker.dto.ApplicationResponse> result =
            applicationService.getApplications(
                testUser,
                ApplicationStatus.INTERVIEW,
                "stripe",
                org.springframework.data.domain.PageRequest.of(0, 10)
            );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).company()).isEqualTo("Stripe");
        assertThat(result.getContent().get(0).notes()).isEqualTo("Great conversation");
    }

    @Test
    @DisplayName("getApplication returns detail with status history")
    void testGetApplicationSuccess() {
        UUID appId = UUID.randomUUID();
        Application app = Application.builder()
            .id(appId)
            .user(testUser)
            .company("Google")
            .role("SRE")
            .currentStatus(ApplicationStatus.INTERVIEW)
            .notes("Screen done")
            .build();

        StatusHistory h1 = StatusHistory.builder()
            .id(UUID.randomUUID())
            .application(app)
            .status(ApplicationStatus.INTERVIEW)
            .changedAt(Instant.now())
            .build();

        when(applicationRepository.findByIdAndUserId(appId, testUser.getId())).thenReturn(Optional.of(app));
        when(statusHistoryRepository.findByApplicationIdOrderByChangedAtDesc(appId)).thenReturn(java.util.List.of(h1));

        com.applicationtracker.dto.ApplicationDetailResponse detail = applicationService.getApplication(testUser, appId);

        assertThat(detail.id()).isEqualTo(appId);
        assertThat(detail.company()).isEqualTo("Google");
        assertThat(detail.statusHistory()).hasSize(1);
    }

    @Test
    @DisplayName("getApplication throws ResourceNotFoundException when not found")
    void testGetApplicationNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(applicationRepository.findByIdAndUserId(nonExistent, testUser.getId())).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
            com.applicationtracker.exception.ResourceNotFoundException.class,
            () -> applicationService.getApplication(testUser, nonExistent)
        );
    }

    @Test
    @DisplayName("updateApplication updates notes and status with new history")
    void testUpdateApplication() {
        UUID appId = UUID.randomUUID();
        Application app = Application.builder()
            .id(appId)
            .user(testUser)
            .company("Figma")
            .role("Designer")
            .currentStatus(ApplicationStatus.APPLIED)
            .build();

        when(applicationRepository.findByIdAndUserId(appId, testUser.getId())).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.findByApplicationIdOrderByChangedAtDesc(appId)).thenReturn(java.util.List.of());

        com.applicationtracker.dto.UpdateApplicationRequest req =
            new com.applicationtracker.dto.UpdateApplicationRequest(ApplicationStatus.INTERVIEW, "Updated notes");

        com.applicationtracker.dto.ApplicationDetailResponse updated =
            applicationService.updateApplication(testUser, appId, req);

        assertThat(updated.currentStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
        assertThat(updated.notes()).isEqualTo("Updated notes");
        verify(statusHistoryRepository).save(any(StatusHistory.class));
    }

    @Test
    @DisplayName("updateApplication rejects UNKNOWN status")
    void testUpdateApplicationRejectsUnknown() {
        UUID appId = UUID.randomUUID();
        Application app = Application.builder()
            .id(appId)
            .user(testUser)
            .company("Figma")
            .currentStatus(ApplicationStatus.APPLIED)
            .build();

        when(applicationRepository.findByIdAndUserId(appId, testUser.getId())).thenReturn(Optional.of(app));

        com.applicationtracker.dto.UpdateApplicationRequest req =
            new com.applicationtracker.dto.UpdateApplicationRequest(ApplicationStatus.UNKNOWN, "Note");

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> applicationService.updateApplication(testUser, appId, req)
        );
    }

    @Test
    @DisplayName("getStats computes correct counts, active count, response rate, and weekly breakdown")
    void testGetStats() {
        Instant now = Instant.now();
        Instant weekAgo = now.minusSeconds(86400 * 7);

        Application app1 = Application.builder().id(UUID.randomUUID()).user(testUser).currentStatus(ApplicationStatus.APPLIED).firstSeenAt(now).build();
        Application app2 = Application.builder().id(UUID.randomUUID()).user(testUser).currentStatus(ApplicationStatus.INTERVIEW).firstSeenAt(now).build();
        Application app3 = Application.builder().id(UUID.randomUUID()).user(testUser).currentStatus(ApplicationStatus.OFFER).firstSeenAt(weekAgo).build();
        Application app4 = Application.builder().id(UUID.randomUUID()).user(testUser).currentStatus(ApplicationStatus.REJECTED).firstSeenAt(weekAgo).build();

        when(applicationRepository.findByUserId(testUser.getId())).thenReturn(java.util.List.of(app1, app2, app3, app4));

        com.applicationtracker.dto.ApplicationStatsResponse stats = applicationService.getStats(testUser);

        assertThat(stats.totalApplications()).isEqualTo(4);
        assertThat(stats.activeApplications()).isEqualTo(2); // APPLIED + INTERVIEW
        assertThat(stats.countsByStatus().get(ApplicationStatus.APPLIED)).isEqualTo(1L);
        assertThat(stats.countsByStatus().get(ApplicationStatus.INTERVIEW)).isEqualTo(1L);
        assertThat(stats.countsByStatus().get(ApplicationStatus.OFFER)).isEqualTo(1L);
        assertThat(stats.countsByStatus().get(ApplicationStatus.REJECTED)).isEqualTo(1L);
        assertThat(stats.responseRate()).isEqualTo(75.0); // 3 out of 4 responded
        assertThat(stats.averageDaysToFirstResponse()).isNotNull();
        assertThat(stats.applicationsPerWeek()).isNotEmpty();
    }

    @Test
    @DisplayName("getStats computes averageDaysToFirstResponse correctly using status history")
    void testGetStatsWithStatusHistory() {
        Instant appliedTime = Instant.now().minusSeconds(86400 * 10);
        Instant responseTime = appliedTime.plusSeconds(86400 * 4); // 4 days later

        UUID appId = UUID.randomUUID();
        Application app = Application.builder()
            .id(appId)
            .user(testUser)
            .currentStatus(ApplicationStatus.INTERVIEW)
            .firstSeenAt(appliedTime)
            .lastUpdatedAt(responseTime)
            .build();

        com.applicationtracker.domain.StatusHistory history = com.applicationtracker.domain.StatusHistory.builder()
            .id(UUID.randomUUID())
            .application(app)
            .status(ApplicationStatus.INTERVIEW)
            .changedAt(responseTime)
            .build();

        when(applicationRepository.findByUserId(testUser.getId())).thenReturn(java.util.List.of(app));
        when(statusHistoryRepository.findByUserIdOrderByChangedAtAsc(testUser.getId()))
            .thenReturn(java.util.List.of(history));

        com.applicationtracker.dto.ApplicationStatsResponse stats = applicationService.getStats(testUser);

        assertThat(stats.totalApplications()).isEqualTo(1);
        assertThat(stats.averageDaysToFirstResponse()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("getPendingFollowups queries applications in APPLIED older than threshold")
    void testGetPendingFollowups() {
        Application app = Application.builder()
            .id(UUID.randomUUID())
            .user(testUser)
            .company("Old Corp")
            .role("Engineer")
            .currentStatus(ApplicationStatus.APPLIED)
            .lastUpdatedAt(Instant.now().minusSeconds(86400 * 10))
            .build();

        when(applicationRepository.findPendingFollowups(eq(testUser.getId()), any(Instant.class)))
            .thenReturn(java.util.List.of(app));

        java.util.List<com.applicationtracker.dto.ApplicationResponse> followups =
            applicationService.getPendingFollowups(testUser, 7);

        assertThat(followups).hasSize(1);
        assertThat(followups.get(0).company()).isEqualTo("Old Corp");
    }
}
