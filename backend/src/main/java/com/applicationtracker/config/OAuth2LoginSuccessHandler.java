package com.applicationtracker.config;

import com.applicationtracker.domain.User;
import com.applicationtracker.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.oauth2.default-redirect-url:http://localhost:5173}")
    private String defaultRedirectUrl = "http://localhost:5173";

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User principal = oauthToken.getPrincipal();
            String email = principal.getAttribute("email");
            String sub = principal.getAttribute("sub");

            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthToken.getName()
            );

            String refreshToken = null;
            if (authorizedClient != null && authorizedClient.getRefreshToken() != null) {
                refreshToken = authorizedClient.getRefreshToken().getTokenValue();
            }

            if (email != null) {
                User user = userService.findOrCreateUser(email, sub, refreshToken);
                log.info("OAuth2 login processed successfully for user id: {}", user.getId());
            } else {
                log.warn("OAuth2 user authenticated without email attribute: {}", principal.getAttributes());
            }
        }

        response.sendRedirect(defaultRedirectUrl);
    }
}
