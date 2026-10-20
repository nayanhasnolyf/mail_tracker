package com.applicationtracker.controller;

import com.applicationtracker.domain.User;
import com.applicationtracker.dto.UserProfileResponse;
import com.applicationtracker.service.GoogleTokenService;
import com.applicationtracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final GoogleTokenService googleTokenService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized", "message", "User is not authenticated"));
        }

        User user = resolveAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user_not_found", "message", "User profile not found in database"));
        }

        UserProfileResponse response = userService.toProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates/refreshes the user's Google OAuth2 access token.
     * If the refresh token has expired or is invalid (e.g. invalid_grant),
     * GoogleTokenService marks the user as reauth_required and returns a clear 401 response.
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized", "message", "User is not authenticated"));
        }

        User user = resolveAuthenticatedUser(authentication);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "user_not_found", "message", "User profile not found"));
        }

        String accessToken = googleTokenService.refreshAccessToken(user);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Token refreshed successfully",
                "accessTokenPreview", accessToken.substring(0, Math.min(10, accessToken.length())) + "..."
        ));
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
