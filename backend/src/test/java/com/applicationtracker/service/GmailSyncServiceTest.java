package com.applicationtracker.service;

import com.applicationtracker.domain.ProcessedEmail;
import com.applicationtracker.domain.SyncState;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.GmailMessageDto;
import com.applicationtracker.dto.SyncResultDto;
import com.applicationtracker.exception.ReauthRequiredException;
import com.applicationtracker.repository.ProcessedEmailRepository;
import com.applicationtracker.repository.SyncStateRepository;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Profile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GmailSyncServiceTest {

    @Mock
    private GmailClient gmailClient;

    @Mock
    private GoogleTokenService googleTokenService;

    @Mock
    private SyncStateRepository syncStateRepository;

    @Mock
    private ProcessedEmailRepository processedEmailRepository;

    private GmailSyncService gmailSyncService;
    private User testUser;

    @BeforeEach
    void setUp() {
        gmailSyncService = new GmailSyncService(
                gmailClient,
                googleTokenService,
                syncStateRepository,
                processedEmailRepository
        );
        ReflectionTestUtils.setField(gmailSyncService, "syncQuery", "subject:(application OR interview)");
        ReflectionTestUtils.setField(gmailSyncService, "syncDays", 90);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("jobseeker@example.com")
                .googleSubjectId("sub-12345")
                .reauthRequired(false)
                .build();
    }

    @Test
    @DisplayName("First sync: should query messages newer than 90 days and initialize historyId")
    void testFirstSync() throws IOException {
        when(googleTokenService.refreshAccessToken(testUser)).thenReturn("valid-access-token");
        when(syncStateRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        Message msg1 = new Message().setId("msg-1");
        Message msg2 = new Message().setId("msg-2");
        when(gmailClient.listMessages(eq("valid-access-token"), eq("subject:(application OR interview) newer_than:90d"), anyLong()))
                .thenReturn(List.of(msg1, msg2));

        Profile profile = new Profile().setHistoryId(BigInteger.valueOf(10050));
        when(gmailClient.getUserProfile("valid-access-token")).thenReturn(profile);

        when(processedEmailRepository.existsByUserIdAndMessageId(testUser.getId(), "msg-1")).thenReturn(false);
        when(processedEmailRepository.existsByUserIdAndMessageId(testUser.getId(), "msg-2")).thenReturn(false);

        GmailMessageDto dto1 = new GmailMessageDto("msg-1", "th-1", BigInteger.valueOf(10040),
                "Application Received", "jobs@meta.com", "me@example.com", "snippet", "body 1", Instant.now());
        GmailMessageDto dto2 = new GmailMessageDto("msg-2", "th-2", BigInteger.valueOf(10045),
                "Interview Scheduling", "talent@apple.com", "me@example.com", "snippet", "body 2", Instant.now());

        when(gmailClient.fetchMessage("valid-access-token", "msg-1")).thenReturn(dto1);
        when(gmailClient.fetchMessage("valid-access-token", "msg-2")).thenReturn(dto2);

        SyncResultDto result = gmailSyncService.syncUser(testUser);

        assertThat(result.isFirstSync()).isTrue();
        assertThat(result.messagesFound()).isEqualTo(2);
        assertThat(result.messagesProcessed()).isEqualTo(2);
        assertThat(result.lastHistoryId()).isEqualTo("10050");

        verify(processedEmailRepository, times(2)).save(any(ProcessedEmail.class));
        verify(syncStateRepository).save(any(SyncState.class));
    }

    @Test
    @DisplayName("Later sync: should call history.list and process newly added messages")
    void testLaterIncrementalSync() throws IOException {
        when(googleTokenService.refreshAccessToken(testUser)).thenReturn("valid-access-token");

        SyncState existingSync = SyncState.of(testUser, "10050", Instant.now().minusSeconds(900));
        when(syncStateRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(existingSync));

        History historyRecord = new History()
                .setId(BigInteger.valueOf(10065))
                .setMessagesAdded(List.of(
                        new HistoryMessageAdded().setMessage(new Message().setId("msg-new-3"))
                ));

        when(gmailClient.listHistory(eq("valid-access-token"), eq(BigInteger.valueOf(10050)), anyLong()))
                .thenReturn(List.of(historyRecord));

        when(processedEmailRepository.existsByUserIdAndMessageId(testUser.getId(), "msg-new-3")).thenReturn(false);

        GmailMessageDto dto3 = new GmailMessageDto("msg-new-3", "th-3", BigInteger.valueOf(10065),
                "Offer Letter", "hr@netflix.com", "me@example.com", "snippet", "body 3", Instant.now());
        when(gmailClient.fetchMessage("valid-access-token", "msg-new-3")).thenReturn(dto3);

        SyncResultDto result = gmailSyncService.syncUser(testUser);

        assertThat(result.isFirstSync()).isFalse();
        assertThat(result.messagesFound()).isEqualTo(1);
        assertThat(result.messagesProcessed()).isEqualTo(1);
        assertThat(result.lastHistoryId()).isEqualTo("10065");

        verify(processedEmailRepository).save(any(ProcessedEmail.class));
        verify(syncStateRepository).save(existingSync);
    }

    @Test
    @DisplayName("Idempotency: should never process an already processed message ID")
    void testSyncIdempotencySkipsProcessedMessages() throws IOException {
        when(googleTokenService.refreshAccessToken(testUser)).thenReturn("valid-access-token");
        when(syncStateRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        Message msg1 = new Message().setId("msg-already-seen");
        when(gmailClient.listMessages(any(), any(), anyLong())).thenReturn(List.of(msg1));
        when(gmailClient.getUserProfile(any())).thenReturn(new Profile().setHistoryId(BigInteger.valueOf(20000)));

        // Marked as already processed in database
        when(processedEmailRepository.existsByUserIdAndMessageId(testUser.getId(), "msg-already-seen")).thenReturn(true);

        SyncResultDto result = gmailSyncService.syncUser(testUser);

        assertThat(result.messagesFound()).isEqualTo(1);
        assertThat(result.messagesProcessed()).isEqualTo(0);

        verify(gmailClient, never()).fetchMessage(any(), eq("msg-already-seen"));
        verify(processedEmailRepository, never()).save(any(ProcessedEmail.class));
    }

    @Test
    @DisplayName("Should throw ReauthRequiredException when user is flagged as requiring reauth")
    void testSyncThrowsWhenReauthRequired() {
        testUser.setReauthRequired(true);

        assertThatThrownBy(() -> gmailSyncService.syncUser(testUser))
                .isInstanceOf(ReauthRequiredException.class)
                .hasMessageContaining("User requires re-authentication");

        verifyNoInteractions(gmailClient);
    }
}
