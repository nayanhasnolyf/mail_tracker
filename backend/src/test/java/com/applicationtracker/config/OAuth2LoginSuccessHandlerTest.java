package com.applicationtracker.config;

import com.applicationtracker.domain.User;
import com.applicationtracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private OAuth2LoginSuccessHandler successHandler;

    @Test
    @DisplayName("Should extract email and refresh token, persist user, and redirect to dashboard")
    void testOnAuthenticationSuccess() throws Exception {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-12345",
                "email", "applicant@gmail.com",
                "name", "Applicant"
        );
        OAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"
        );

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                oAuth2User,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                "google"
        );

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken("mock-refresh-token-value", Instant.now());
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "mock-access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );

        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                mock(org.springframework.security.oauth2.client.registration.ClientRegistration.class),
                "google-sub-12345",
                accessToken,
                refreshToken
        );

        when(authorizedClientService.loadAuthorizedClient("google", "google-sub-12345"))
                .thenReturn(authorizedClient);

        when(userService.findOrCreateUser("applicant@gmail.com", "google-sub-12345", "mock-refresh-token-value"))
                .thenReturn(User.builder().id(UUID.randomUUID()).email("applicant@gmail.com").build());

        successHandler.onAuthenticationSuccess(request, response, authenticationToken);

        verify(userService).findOrCreateUser("applicant@gmail.com", "google-sub-12345", "mock-refresh-token-value");
        verify(response).sendRedirect(anyString());
    }
}
