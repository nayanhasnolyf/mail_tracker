package com.applicationtracker.controller;

import com.applicationtracker.domain.User;
import com.applicationtracker.dto.SyncResultDto;
import com.applicationtracker.service.GmailSyncService;
import com.applicationtracker.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SyncController {

    private final UserService userService;
    private final GmailSyncService gmailSyncService;

    /**
     * Triggers a manual sync for the authenticated user.
     */
    @PostMapping("/sync")
    public ResponseEntity<?> triggerManualSync(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized", "message", "User is not authenticated"));
        }

        User user = resolveAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user_not_found", "message", "User profile not found in database"));
        }

        SyncResultDto result = gmailSyncService.syncUser(user);
        return ResponseEntity.ok(result);
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        String email = null;
        String sub = null;

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
            sub = oauth2User.getAttribute("sub");
        } else {
            email = authentication.getName();
        }

        if (sub != null && !sub.isBlank()) {
            Optional<User> bySub = userService.findByGoogleSubjectId(sub);
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }

        if (email != null && !email.isBlank()) {
            return userService.findByEmail(email).orElse(null);
        }

        return null;
    }
}
