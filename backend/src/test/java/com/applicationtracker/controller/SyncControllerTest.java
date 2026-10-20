package com.applicationtracker.controller;

import com.applicationtracker.config.GlobalExceptionHandler;
import com.applicationtracker.config.SecurityConfig;
import com.applicationtracker.domain.User;
import com.applicationtracker.dto.SyncResultDto;
import com.applicationtracker.exception.ReauthRequiredException;
import com.applicationtracker.service.GmailSyncService;
import com.applicationtracker.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SyncController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private GmailSyncService gmailSyncService;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("POST /api/sync should return 401 Unauthorized when unauthenticated")
    void testSyncUnauthenticatedReturns401() throws Exception {
        mockMvc.perform(post("/api/sync").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/sync should trigger sync and return 200 OK for authenticated user")
    void testSyncAuthenticatedReturnsSyncResult() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("applicant@example.com")
                .googleSubjectId("sub-555")
                .build();

        SyncResultDto resultDto = new SyncResultDto(
                userId,
                "SUCCESS",
                true,
                5,
                5,
                "10200",
                Instant.now()
        );

        when(userService.findByGoogleSubjectId("sub-555")).thenReturn(Optional.of(user));
        when(gmailSyncService.syncUser(user)).thenReturn(resultDto);

        mockMvc.perform(post("/api/sync")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> {
                            attrs.put("sub", "sub-555");
                            attrs.put("email", "applicant@example.com");
                        })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.messagesFound").value(5))
                .andExpect(jsonPath("$.messagesProcessed").value(5))
                .andExpect(jsonPath("$.lastHistoryId").value("10200"));
    }

    @Test
    @DisplayName("POST /api/sync should return 401 reauth_required when user token is expired or revoked")
    void testSyncWhenReauthRequiredReturns401() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("revoked@example.com")
                .googleSubjectId("sub-revoked")
                .build();

        when(userService.findByGoogleSubjectId("sub-revoked")).thenReturn(Optional.of(user));
        when(gmailSyncService.syncUser(any(User.class)))
                .thenThrow(new ReauthRequiredException("Google authorization has expired or was revoked. Please log in again."));

        mockMvc.perform(post("/api/sync")
                        .with(csrf())
                        .with(oauth2Login().attributes(attrs -> {
                            attrs.put("sub", "sub-revoked");
                            attrs.put("email", "revoked@example.com");
                        })))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("reauth_required"))
                .andExpect(jsonPath("$.message").value("Google authorization has expired or was revoked. Please log in again."));
    }
}
