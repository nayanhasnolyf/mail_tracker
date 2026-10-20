package com.applicationtracker.service;

import com.applicationtracker.domain.User;
import com.applicationtracker.exception.ReauthRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Service to refresh Google OAuth2 access tokens and handle token expiration / revocation.
 */
@Slf4j
@Service
public class GoogleTokenService {

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";

    private final UserService userService;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public GoogleTokenService(
            UserService userService,
            RestClient.Builder restClientBuilder,
            @Value("${spring.security.oauth2.client.registration.google.client-id:}") String clientId,
            @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String clientSecret) {
        this.userService = userService;
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Refreshes the Google OAuth2 access token for a user using their encrypted refresh token.
     * If the token has expired or is revoked (e.g. invalid_grant), marks the user as reauth_required
     * and throws ReauthRequiredException.
     */
    public String refreshAccessToken(User user) {
        String decryptedRefreshToken = userService.getDecryptedRefreshToken(user);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", decryptedRefreshToken);
        body.add("grant_type", "refresh_token");

        try {
            Map<String, Object> response = restClient.post()
                    .uri(GOOGLE_TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
            throw new IllegalStateException("Google token response did not contain access_token");
        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            log.warn("Failed to refresh Google token for user {}. Response: {}", user.getEmail(), responseBody);

            if (isInvalidGrant(e.getStatusCode().value(), responseBody)) {
                userService.markReauthRequired(user);
                throw new ReauthRequiredException(
                        "Google authorization has expired or was revoked. Please log in again."
                );
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error refreshing token for user {}", user.getEmail(), e);
            throw e;
        }
    }

    /**
     * Checks if the response indicates an invalid_grant / expired or revoked refresh token.
     */
    public boolean isInvalidGrant(int statusCode, String responseBody) {
        if (statusCode == 400 || statusCode == 401) {
            return responseBody != null && (
                    responseBody.contains("invalid_grant") ||
                    responseBody.contains("Token has been expired or revoked") ||
                    responseBody.contains("token_expired")
            );
        }
        return false;
    }
}
