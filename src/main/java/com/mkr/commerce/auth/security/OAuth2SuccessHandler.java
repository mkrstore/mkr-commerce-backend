package com.mkr.commerce.auth.security;

import com.mkr.commerce.auth.dto.AuthTokenPair;
import com.mkr.commerce.auth.service.AuthService;
import com.mkr.commerce.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String REFRESH_COOKIE = "mkr_refresh_token";

    private final AuthService authService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest  request,
                                         HttpServletResponse response,
                                         Authentication      authentication) throws IOException {
        try {
            // Google uses OIDC (openid scope) → OidcUserPrincipal
            // Other OAuth2 providers → OAuth2UserPrincipal
            Object raw = authentication.getPrincipal();
            User user;
            if (raw instanceof OidcUserPrincipal p) {
                user = p.getUser();
            } else if (raw instanceof OAuth2UserPrincipal p) {
                user = p.getUser();
            } else {
                throw new IllegalStateException("Unexpected OAuth2 principal type: " + raw.getClass().getName());
            }

            AuthTokenPair pair = authService.loginWithGoogle(user);

            Cookie cookie = new Cookie(REFRESH_COOKIE, pair.rawRefreshToken());
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "None");
            cookie.setPath("/api/auth");
            cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
            response.addCookie(cookie);

            HttpSession session = request.getSession(false);
            if (session != null) session.invalidate();

            String redirect = UriComponentsBuilder
                    .fromUriString(baseUrl + "/oauth2/callback")
                    .queryParam("token", pair.accessToken())
                    .build().toUriString();

            log.info("OAuth2 login success: {} [{}]", user.getEmail(), user.getRole());
            response.sendRedirect(redirect);

        } catch (Exception ex) {
            log.error("OAuth2 success handler error — redirecting to login", ex);
            response.sendRedirect(baseUrl + "/login?error=server_error");
        }
    }
}
