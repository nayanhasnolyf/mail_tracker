package com.applicationtracker.service;

import com.applicationtracker.domain.User;
import com.applicationtracker.exception.ReauthRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleTokenServiceTest {

    @Mock
    private UserService userService;

    private GoogleTokenService googleTokenService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        googleTokenService = new GoogleTokenService(
                userService,
                restClientBuilder,
                "test-client-id",
                "test-client-secret"
        );
    }

    @Test
    @DisplayName("Should successfully refresh access token using decrypted refresh token")
    void testRefreshAccessTokenSuccess() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .encryptedRefreshToken("enc-token")
                .build();

        when(userService.getDecryptedRefreshToken(user)).thenReturn("decrypted-token-value");

        String mockResponseJson = """
                {
                    "access_token": "ya29.new-access-token-12345",
                    "expires_in": 3599,
                    "token_type": "Bearer"
                }
                """;

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

        String newAccessToken = googleTokenService.refreshAccessToken(user);

        assertThat(newAccessToken).isEqualTo("ya29.new-access-token-12345");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should detect invalid_grant, mark user as reauth_required, and throw ReauthRequiredException")
    void testRefreshAccessTokenInvalidGrantMarksReauthRequired() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .encryptedRefreshToken("enc-token")
                .reauthRequired(false)
                .build();

        when(userService.getDecryptedRefreshToken(user)).thenReturn("expired-refresh-token");

        String invalidGrantErrorJson = """
                {
                    "error": "invalid_grant",
                    "error_description": "Token has been expired or revoked."
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .headers(headers)
                        .body(invalidGrantErrorJson.getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> googleTokenService.refreshAccessToken(user))
                .isInstanceOf(ReauthRequiredException.class)
                .hasMessageContaining("Google authorization has expired or was revoked");

        verify(userService).markReauthRequired(user);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should identify invalid_grant status codes and payloads")
    void testIsInvalidGrantDetection() {
        assertThat(googleTokenService.isInvalidGrant(400, "{\"error\": \"invalid_grant\"}")).isTrue();
        assertThat(googleTokenService.isInvalidGrant(400, "Token has been expired or revoked")).isTrue();
        assertThat(googleTokenService.isInvalidGrant(500, "{\"error\": \"internal_error\"}")).isFalse();
        assertThat(googleTokenService.isInvalidGrant(200, "{}")).isFalse();
    }
}
