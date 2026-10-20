package com.applicationtracker.repository;

import com.applicationtracker.domain.Application;
import com.applicationtracker.domain.ApplicationStatus;
import com.applicationtracker.domain.StatusHistory;
import com.applicationtracker.domain.SyncState;
import com.applicationtracker.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgreSqlRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("application_tracker_test")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureDatabaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StatusHistoryRepository statusHistoryRepository;

    @Autowired
    private SyncStateRepository syncStateRepository;

    @Test
    @DisplayName("Should run schema migrations and persist domain entities with constraints in PostgreSQL")
    void testFullDomainLifecycleOnPostgreSql() {
        // 1. Create and verify user
        User user = userRepository.saveAndFlush(User.builder()
                .email("tc-user@example.com")
                .googleSubjectId("tc-sub-999")
                .build());

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();

        // Verify unique email constraint on PostgreSQL
        User duplicateUser = User.builder()
                .email("tc-user@example.com")
                .googleSubjectId("tc-sub-different")
                .build();
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicateUser))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 2. Create and verify application
        Application app = applicationRepository.saveAndFlush(Application.builder()
                .user(user)
                .company("Stripe")
                .role("Infrastructure Engineer")
                .currentStatus(ApplicationStatus.APPLIED)
                .sourceEmailId("email-stripe-001")
                .build());

        assertThat(app.getId()).isNotNull();
        assertThat(app.getFirstSeenAt()).isNotNull();
        assertThat(app.getLastUpdatedAt()).isNotNull();

        // Verify unique constraint (user_id, source_email_id) on PostgreSQL
        Application duplicateApp = Application.builder()
                .user(user)
                .company("Another Corp")
                .role("DevOps")
                .currentStatus(ApplicationStatus.APPLIED)
                .sourceEmailId("email-stripe-001")
                .build();
        assertThatThrownBy(() -> applicationRepository.saveAndFlush(duplicateApp))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 3. Create status history
        StatusHistory history = statusHistoryRepository.saveAndFlush(StatusHistory.builder()
                .application(app)
                .status(ApplicationStatus.ASSESSMENT)
                .sourceEmailId("email-stripe-assessment")
                .build());

        assertThat(history.getId()).isNotNull();
        assertThat(history.getStatus()).isEqualTo(ApplicationStatus.ASSESSMENT);

        // Verify unique constraint (application_id, source_email_id) on PostgreSQL
        StatusHistory duplicateHistory = StatusHistory.builder()
                .application(app)
                .status(ApplicationStatus.INTERVIEW)
                .sourceEmailId("email-stripe-assessment")
                .build();
        assertThatThrownBy(() -> statusHistoryRepository.saveAndFlush(duplicateHistory))
                .isInstanceOf(DataIntegrityViolationException.class);

        // 4. Create and verify SyncState
        SyncState syncState = syncStateRepository.saveAndFlush(
                SyncState.of(user, "gmail-history-42", Instant.now()));

        assertThat(syncState.getUserId()).isEqualTo(user.getId());
        Optional<SyncState> retrievedSync = syncStateRepository.findByUserId(user.getId());
        assertThat(retrievedSync).isPresent();
        assertThat(retrievedSync.get().getLastHistoryId()).isEqualTo("gmail-history-42");
    }
}
