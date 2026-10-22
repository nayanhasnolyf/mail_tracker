package com.applicationtracker.service;

import com.applicationtracker.domain.User;
import com.applicationtracker.dto.UserProfileResponse;
import com.applicationtracker.exception.ReauthRequiredException;
import com.applicationtracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    /**
     * Finds an existing user by Google Subject ID or email, or creates a new user.
     * Updates encrypted refresh token if a new one is provided.
     */
    @Transactional
    public User findOrCreateUser(String email, String googleSubjectId, String rawRefreshToken) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("User email cannot be null or blank");
        }

        Optional<User> existingUserOpt = Optional.empty();
        if (googleSubjectId != null && !googleSubjectId.isBlank()) {
            existingUserOpt = userRepository.findByGoogleSubjectId(googleSubjectId);
        }
        if (existingUserOpt.isEmpty()) {
            existingUserOpt = userRepository.findByEmail(email);
        }

        if (existingUserOpt.isPresent()) {
            User user = existingUserOpt.get();
            if (googleSubjectId != null && (user.getGoogleSubjectId() == null || !user.getGoogleSubjectId().equals(googleSubjectId))) {
                user.setGoogleSubjectId(googleSubjectId);
            }

            if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
                user.setEncryptedRefreshToken(encryptionService.encrypt(rawRefreshToken));
                user.setReauthRequired(false);
                log.info("Updated encrypted refresh token for user {}", email);
            } else if (user.isReauthRequired()) {
                // If user logged in interactively again, clear reauth flag
                user.setReauthRequired(false);
            }

            return userRepository.save(user);
        }

        // Create new user
        String encryptedToken = (rawRefreshToken != null && !rawRefreshToken.isBlank())
                ? encryptionService.encrypt(rawRefreshToken)
                : null;

        User newUser = User.builder()
                .email(email)
                .googleSubjectId(googleSubjectId)
                .encryptedRefreshToken(encryptedToken)
                .reauthRequired(false)
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created new user {} with googleSubjectId {}", email, googleSubjectId);
        return savedUser;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByGoogleSubjectId(String googleSubjectId) {
        return userRepository.findByGoogleSubjectId(googleSubjectId);
    }

    /**
     * Marks the user as requiring re-authentication when refresh token is invalid or expired.
     */
    @Transactional
    public User markReauthRequired(User user) {
        user.setReauthRequired(true);
        User saved = userRepository.save(user);
        log.warn("Marked user {} as reauth required", user.getEmail());
        return saved;
    }

    /**
     * Retrieves and decrypts the user's stored refresh token.
     * Throws ReauthRequiredException if token is missing.
     */
    @Transactional(readOnly = true)
    public String getDecryptedRefreshToken(User user) {
        if (user.getEncryptedRefreshToken() == null || user.getEncryptedRefreshToken().isBlank()) {
            throw new ReauthRequiredException("No refresh token available. Re-authentication required.");
        }
        try {
            return encryptionService.decrypt(user.getEncryptedRefreshToken());
        } catch (Exception e) {
            log.error("Failed to decrypt refresh token for user {}", user.getEmail(), e);
            throw new ReauthRequiredException("Failed to decrypt stored token. Re-authentication required.");
        }
    }

    @Transactional(readOnly = true)
    public User resolveAuthenticatedUser(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = null;
        String sub = null;

        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
            sub = oauth2User.getAttribute("sub");
        } else {
            email = authentication.getName();
        }

        if (sub != null && !sub.isBlank()) {
            Optional<User> bySub = findByGoogleSubjectId(sub);
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }

        if (email != null && !email.isBlank()) {
            return findByEmail(email).orElse(null);
        }

        return null;
    }

    public UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getGoogleSubjectId(),
                user.getCreatedAt(),
                user.isReauthRequired(),
                user.getEncryptedRefreshToken() != null && !user.getEncryptedRefreshToken().isBlank()
        );
    }
}
