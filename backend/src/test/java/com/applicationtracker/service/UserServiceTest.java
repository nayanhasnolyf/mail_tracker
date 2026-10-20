package com.applicationtracker.service;

import com.applicationtracker.domain.User;
import com.applicationtracker.exception.ReauthRequiredException;
import com.applicationtracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionService encryptionService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, encryptionService);
    }

    @Test
    @DisplayName("Should create new user with encrypted refresh token when user does not exist")
    void testFindOrCreateUserCreatesNewUser() {
        when(userRepository.findByGoogleSubjectId("sub-123")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(encryptionService.encrypt("raw-refresh-token")).thenReturn("enc-token-xyz");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User user = userService.findOrCreateUser("test@example.com", "sub-123", "raw-refresh-token");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getGoogleSubjectId()).isEqualTo("sub-123");
        assertThat(user.getEncryptedRefreshToken()).isEqualTo("enc-token-xyz");
        assertThat(user.isReauthRequired()).isFalse();

        verify(encryptionService).encrypt("raw-refresh-token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should find and update existing user by google subject ID")
    void testFindOrCreateUserUpdatesExistingUser() {
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .googleSubjectId("sub-123")
                .reauthRequired(true)
                .build();

        when(userRepository.findByGoogleSubjectId("sub-123")).thenReturn(Optional.of(existingUser));
        when(encryptionService.encrypt("new-refresh-token")).thenReturn("new-enc-token");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.findOrCreateUser("test@example.com", "sub-123", "new-refresh-token");

        assertThat(user.getEncryptedRefreshToken()).isEqualTo("new-enc-token");
        assertThat(user.isReauthRequired()).isFalse();
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("Should mark user as reauth required")
    void testMarkReauthRequired() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .reauthRequired(false)
                .build();

        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.markReauthRequired(user);
        assertThat(updated.isReauthRequired()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should throw ReauthRequiredException when refresh token is missing or null")
    void testGetDecryptedRefreshTokenThrowsWhenMissing() {
        User user = User.builder()
                .email("test@example.com")
                .encryptedRefreshToken(null)
                .build();

        assertThatThrownBy(() -> userService.getDecryptedRefreshToken(user))
                .isInstanceOf(ReauthRequiredException.class)
                .hasMessageContaining("No refresh token available");
    }
}
