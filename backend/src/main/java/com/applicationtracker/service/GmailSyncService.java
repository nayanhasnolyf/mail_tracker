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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Service orchestrating Gmail synchronization with idempotency and history tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmailSyncService {

    private final GmailClient gmailClient;
    private final GoogleTokenService googleTokenService;
    private final SyncStateRepository syncStateRepository;
    private final ProcessedEmailRepository processedEmailRepository;

    @Value("${app.gmail.sync-query:subject:(application OR interview OR assessment OR regret OR offer)}")
    private String syncQuery;

    @Value("${app.gmail.sync-days:90}")
    private int syncDays;

    /**
     * Synchronizes messages for the given user.
     * Uses query search for first sync, or history.list for subsequent syncs.
     * Guaranteed to be idempotent via ProcessedEmail tracking.
     */
    @Transactional
    public SyncResultDto syncUser(User user) {
        if (user.isReauthRequired()) {
            throw new ReauthRequiredException("User requires re-authentication before syncing Gmail.");
        }

        String accessToken = googleTokenService.refreshAccessToken(user);
        SyncState syncState = syncStateRepository.findByUserId(user.getId())
                .orElseGet(() -> SyncState.of(user, null, null));

        boolean isFirstSync = (syncState.getLastHistoryId() == null || syncState.getLastHistoryId().isBlank());
        Set<String> messageIdsToProcess = new LinkedHashSet<>();
        String newHistoryId = null;

        try {
            if (isFirstSync) {
                log.info("Performing first Gmail sync for user {}", user.getEmail());
                String fullQuery = String.format("%s newer_than:%dd", syncQuery, syncDays);
                List<Message> messages = gmailClient.listMessages(accessToken, fullQuery, 200L);
                for (Message msg : messages) {
                    if (msg.getId() != null) {
                        messageIdsToProcess.add(msg.getId());
                    }
                }

                Profile profile = gmailClient.getUserProfile(accessToken);
                if (profile != null && profile.getHistoryId() != null) {
                    newHistoryId = profile.getHistoryId().toString();
                }
            } else {
                log.info("Performing incremental history sync for user {} from historyId {}",
                        user.getEmail(), syncState.getLastHistoryId());
                try {
                    BigInteger startHistoryId = new BigInteger(syncState.getLastHistoryId());
                    List<History> historyList = gmailClient.listHistory(accessToken, startHistoryId, 200L);
                    for (History h : historyList) {
                        if (h.getMessagesAdded() != null) {
                            for (HistoryMessageAdded added : h.getMessagesAdded()) {
                                if (added.getMessage() != null && added.getMessage().getId() != null) {
                                    messageIdsToProcess.add(added.getMessage().getId());
                                }
                            }
                        }
                        if (h.getId() != null) {
                            newHistoryId = h.getId().toString();
                        }
                    }

                    if (newHistoryId == null) {
                        Profile profile = gmailClient.getUserProfile(accessToken);
                        if (profile != null && profile.getHistoryId() != null) {
                            newHistoryId = profile.getHistoryId().toString();
                        }
                    }
                } catch (Exception e) {
                    log.warn("History sync failed for user {} (possibly expired historyId). Falling back to query: {}",
                            user.getEmail(), e.getMessage());
                    String fallbackQuery = String.format("%s newer_than:7d", syncQuery);
                    List<Message> messages = gmailClient.listMessages(accessToken, fallbackQuery, 50L);
                    for (Message msg : messages) {
                        if (msg.getId() != null) {
                            messageIdsToProcess.add(msg.getId());
                        }
                    }
                    Profile profile = gmailClient.getUserProfile(accessToken);
                    if (profile != null && profile.getHistoryId() != null) {
                        newHistoryId = profile.getHistoryId().toString();
                    }
                }
            }
        } catch (IOException e) {
            log.error("IO error interacting with Gmail API for user {}", user.getEmail(), e);
            throw new IllegalStateException("Failed to communicate with Gmail API: " + e.getMessage(), e);
        }

        int processedCount = 0;
        for (String messageId : messageIdsToProcess) {
            // Idempotency check: never process the same message id twice
            if (processedEmailRepository.existsByUserIdAndMessageId(user.getId(), messageId)) {
                log.debug("Message {} already processed for user {}. Skipping.", messageId, user.getEmail());
                continue;
            }

            try {
                GmailMessageDto messageDto = gmailClient.fetchMessage(accessToken, messageId);

                processedEmailRepository.save(ProcessedEmail.builder()
                        .user(user)
                        .messageId(messageId)
                        .subject(truncate(messageDto.subject(), 500))
                        .sender(truncate(messageDto.from(), 255))
                        .build());

                processedCount++;
                log.debug("Processed message {} with subject '{}'", messageId, messageDto.subject());
            } catch (Exception e) {
                log.error("Failed to process message {} for user {}", messageId, user.getEmail(), e);
            }
        }

        if (newHistoryId != null) {
            syncState.setLastHistoryId(newHistoryId);
        }
        syncState.setLastSyncedAt(Instant.now());
        syncStateRepository.save(syncState);

        log.info("Gmail sync completed for user {}: found {}, processed {} new, historyId={}",
                user.getEmail(), messageIdsToProcess.size(), processedCount, syncState.getLastHistoryId());

        return new SyncResultDto(
                user.getId(),
                "SUCCESS",
                isFirstSync,
                messageIdsToProcess.size(),
                processedCount,
                syncState.getLastHistoryId(),
                syncState.getLastSyncedAt()
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
