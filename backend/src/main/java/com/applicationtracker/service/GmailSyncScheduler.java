package com.applicationtracker.service;

import com.applicationtracker.domain.User;
import com.applicationtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler running every 15 minutes to synchronize Gmail messages for active users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GmailSyncScheduler {

    private final UserRepository userRepository;
    private final GmailSyncService gmailSyncService;

    @Scheduled(fixedRateString = "${app.gmail.sync-rate-ms:900000}", initialDelay = 60000)
    public void runScheduledSync() {
        log.info("Starting scheduled 15-minute Gmail synchronization cycle...");
        List<User> users = userRepository.findAll();

        for (User user : users) {
            if (user.isReauthRequired() || user.getEncryptedRefreshToken() == null || user.getEncryptedRefreshToken().isBlank()) {
                continue;
            }

            try {
                gmailSyncService.syncUser(user);
            } catch (Exception e) {
                log.error("Scheduled sync failed for user {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}
