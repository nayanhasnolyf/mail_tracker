package com.applicationtracker.controller;

import com.applicationtracker.config.GlobalExceptionHandler;
import com.applicationtracker.config.SecurityConfig;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.UserProfileResponse;
import com.applicationtracker.exception.ReauthRequiredException;
import com.applicationtracker.service.GoogleTokenService;
import com.applicationtracker.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private GoogleTokenService googleTokenService;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("GET /api/me should return 401 Unauthorized when unauthenticated")
    void testGetMeUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/me should return user profile when authenticated with mocked OAuth2")
    void testGetMeAuthenticatedReturnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test.applicant@example.com")
                .googleSubjectId("google-sub-777")
                .createdAt(Instant.now())
                .encryptedRefreshToken("encrypted-token-xyz")
                .reauthRequired(false)
                .build();

        UserProfileResponse response = new UserProfileResponse(
                userId,
                "test.applicant@example.com",
                "google-sub-777",
                Instant.now(),
                false,
                true
        );

        when(userService.findByGoogleSubjectId("google-sub-777")).thenReturn(Optional.of(user));
        when(userService.toProfileResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/me")
                        .with(oauth2Login()
                                .attributes(attrs -> {
                                    attrs.put("sub", "google-sub-777");
                                    attrs.put("email", "test.applicant@example.com");
                                })
                                .authorities(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("test.applicant@example.com"))
                .andExpect(jsonPath("$.googleSubjectId").value("google-sub-777"))
                .andExpect(jsonPath("$.reauthRequired").value(false))
                .andExpect(jsonPath("$.hasRefreshToken").value(true));
    }

    @Test
    @DisplayName("GET /api/me should show reauthRequired: true when user token has expired")
    void testGetMeWhenReauthRequiredReturnsTrue() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("expired.user@example.com")
                .googleSubjectId("google-sub-888")
                .createdAt(Instant.now())
                .encryptedRefreshToken("expired-token")
                .reauthRequired(true)
                .build();

        UserProfileResponse response = new UserProfileResponse(
                userId,
                "expired.user@example.com",
                "google-sub-888",
                Instant.now(),
                true,
                true
        );

        when(userService.findByGoogleSubjectId("google-sub-888")).thenReturn(Optional.of(user));
        when(userService.toProfileResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/me")
                        .with(oauth2Login()
                                .attributes(attrs -> {
                                    attrs.put("sub", "google-sub-888");
                                    attrs.put("email", "expired.user@example.com");
                                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("expired.user@example.com"))
                .andExpect(jsonPath("$.reauthRequired").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/refresh should return 401 reauth_required when Google returns invalid_grant")
    void testRefreshEndpointHandlesInvalidGrant() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .googleSubjectId("sub-123")
                .reauthRequired(false)
                .build();

        when(userService.findByGoogleSubjectId("sub-123")).thenReturn(Optional.of(user));
        when(googleTokenService.refreshAccessToken(any(User.class)))
                .thenThrow(new ReauthRequiredException("Google authorization has expired or was revoked. Please log in again."));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .with(oauth2Login()
                                .attributes(attrs -> {
                                    attrs.put("sub", "sub-123");
                                    attrs.put("email", "user@example.com");
                                })))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("reauth_required"))
                .andExpect(jsonPath("$.message").value("Google authorization has expired or was revoked. Please log in again."))
                .andExpect(jsonPath("$.authUrl").value("/oauth2/authorization/google"));
    }
}
