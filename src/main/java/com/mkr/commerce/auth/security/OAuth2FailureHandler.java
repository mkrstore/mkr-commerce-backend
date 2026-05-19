package com.mkr.commerce.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final String REFRESH_COOKIE = "mkr_refresh_token";

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest  request,
                                         HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        log.warn("OAuth2 login failed: {}", exception.getMessage());

        // Clear any stale refresh token cookie so initAuth() finds nothing on reload.
        Cookie clear = new Cookie(REFRESH_COOKIE, "");
        clear.setHttpOnly(true);
        clear.setPath("/api/auth");
        clear.setMaxAge(0);
        response.addCookie(clear);

        String errorCode = "oauth_failed";
        if (exception instanceof OAuth2AuthenticationException ex && ex.getError() != null) {
            errorCode = ex.getError().getErrorCode();
        }

        response.sendRedirect(baseUrl + "/login?error=" + errorCode);
    }
}
