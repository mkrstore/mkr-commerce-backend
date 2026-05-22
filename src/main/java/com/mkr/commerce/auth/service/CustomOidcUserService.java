package com.mkr.commerce.auth.service;

import com.mkr.commerce.auth.security.OidcUserPrincipal;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.repository.InvitationTokenRepository;
import com.mkr.commerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles Google Sign-In when the openid scope is present (OIDC flow).
 * Spring calls this instead of {@link CustomOAuth2UserService} for OIDC providers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final UserRepository           userRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final OidcUserService           delegate = new OidcUserService();

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(request);

        String googleId = oidcUser.getSubject();
        String email    = oidcUser.getEmail();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_email"),
                    "Google did not return an email address.");
        }

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email.toLowerCase()))
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("user_not_found"),
                        "No staff account found for " + email + ". Contact your administrator."));

        if (!user.isActive()) {
            // Distinguish: pending invitation (never activated) vs explicitly deactivated by admin
            boolean hasPendingInvitation = invitationTokenRepository
                    .findActiveByUser(user, Instant.now()).isPresent();
            if (!hasPendingInvitation) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("account_deactivated"),
                        "Your account has been deactivated. Contact your administrator.");
            }
            // User has a pending invitation — allow Google sign-in, success handler will redirect to set-password
            log.info("Google sign-in for pending user: {} — will redirect to set-password", user.getEmail());
        }

        if (user.getGoogleId() == null) {
            linkGoogleId(user.getId(), googleId);
            log.info("Linked Google account for: {}", user.getEmail());
        }

        return new OidcUserPrincipal(user, oidcUser);
    }

    @Transactional
    public void linkGoogleId(UUID userId, String googleId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setGoogleId(googleId);
            userRepository.save(u);
        });
    }
}
