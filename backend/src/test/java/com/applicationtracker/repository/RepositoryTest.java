package com.applicationtracker.repository;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.StatusHistory;
import com.applicationtracker.domain.SyncState;
import com.applicationtracker.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SyncStateRepository syncStateRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("candidate@example.com")
                .googleSubjectId("google-sub-12345")
                .build());
        entityManager.flush();
    }

    @Test
    @DisplayName("Should save and find user by email and google subject ID")
    void testSaveAndFindUser() {
        Optional<User> foundByEmail = userRepository.findByEmail("candidate@example.com");
        assertThat(foundByEmail).isPresent();
        assertThat(foundByEmail.get().getId()).isNotNull();
        assertThat(foundByEmail.get().getCreatedAt()).isNotNull();

        Optional<User> foundBySub = userRepository.findByGoogleSubjectId("google-sub-12345");
        assertThat(foundBySub).isPresent();
        assertThat(userRepository.existsByEmail("candidate@example.com")).isTrue();
    }

    @Test
    @DisplayName("Should reject duplicate user email due to unique constraint")
    void testDuplicateUserEmailThrowsException() {
        User duplicateUser = User.builder()
                .email("candidate@example.com")
                .googleSubjectId("google-sub-different")
                .build();

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(duplicateUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should save application and find by user and status")
    void testSaveAndFindApplication() {
        Application application = applicationRepository.saveAndFlush(Application.builder()
                .user(testUser)
                .company("Acme Corp")
                .role("Senior Software Engineer")
                .currentStatus(ApplicationStatus.APPLIED)
                .sourceEmailId("msg-101")
                .build());

        assertThat(application.getId()).isNotNull();
        assertThat(application.getFirstSeenAt()).isNotNull();
        assertThat(application.getLastUpdatedAt()).isNotNull();

        List<Application> appliedList = applicationRepository.findByUserIdAndCurrentStatus(
                testUser.getId(), ApplicationStatus.APPLIED);
        assertThat(appliedList).hasSize(1);
        assertThat(appliedList.getFirst().getCompany()).isEqualTo("Acme Corp");

        Optional<Application> foundByEmailId = applicationRepository.findByUserIdAndSourceEmailId(
                testUser.getId(), "msg-101");
        assertThat(foundByEmailId).isPresent();
    }

    @Test
    @DisplayName("Should reject duplicate application with same user and source_email_id")
    void testDuplicateApplicationSourceEmailThrowsException() {
        applicationRepository.saveAndFlush(Application.builder()
                .user(testUser)
                .company("Company A")
                .role("Backend Engineer")
                .currentStatus(ApplicationStatus.APPLIED)
                .sourceEmailId("email-unique-123")
                .build());

        Application duplicateApp = Application.builder()
                .user(testUser)
                .company("Company B")
                .role("Fullstack Engineer")
                .currentStatus(ApplicationStatus.APPLIED)
                .sourceEmailId("email-unique-123")
                .build();

        assertThatThrownBy(() -> {
            applicationRepository.saveAndFlush(duplicateApp);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should save status history with enum progression and reject duplicate email per application")
    void testStatusHistoryLifecycleAndUniqueConstraint() {
        Application app = applicationRepository.saveAndFlush(Application.builder()
                .user(testUser)
                .company("Google")
                .role("Software Engineer")
                .currentStatus(ApplicationStatus.APPLIED)
                .sourceEmailId("msg-init")
                .build());

        statusHistoryRepository.saveAndFlush(StatusHistory.builder()
                .application(app)
                .status(ApplicationStatus.APPLIED)
                .sourceEmailId("msg-init")
                .build());

        statusHistoryRepository.saveAndFlush(StatusHistory.builder()
                .application(app)
                .status(ApplicationStatus.INTERVIEW)
                .sourceEmailId("msg-interview-invite")
                .build());

        List<StatusHistory> history = statusHistoryRepository.findByApplicationIdOrderByChangedAtDesc(app.getId());
        assertThat(history).hasSize(2);
        assertThat(history.getFirst().getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);

        StatusHistory duplicateHistory = StatusHistory.builder()
                .application(app)
                .status(ApplicationStatus.ASSESSMENT)
                .sourceEmailId("msg-interview-invite")
                .build();

        assertThatThrownBy(() -> {
            statusHistoryRepository.saveAndFlush(duplicateHistory);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should save and retrieve SyncState using user primary key")
    void testSaveAndFindSyncState() {
        Instant now = Instant.now();
        SyncState syncState = syncStateRepository.saveAndFlush(
                SyncState.of(testUser, "history-998877", now));

        assertThat(syncState.getUserId()).isEqualTo(testUser.getId());

        Optional<SyncState> found = syncStateRepository.findByUserId(testUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getLastHistoryId()).isEqualTo("history-998877");
        assertThat(found.get().getLastSyncedAt()).isNotNull();
    }
}
