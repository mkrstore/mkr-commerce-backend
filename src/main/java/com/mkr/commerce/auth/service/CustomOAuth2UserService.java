package com.mkr.commerce.auth.service;

import com.mkr.commerce.auth.security.OAuth2UserPrincipal;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // HTTP call to Google — must NOT be inside a transaction
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");

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
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_deactivated"),
                    "Your account has been deactivated. Contact your administrator.");
        }

        if (user.getGoogleId() == null) {
            linkGoogleId(user.getId(), googleId);
            log.info("Linked Google account for: {}", user.getEmail());
        }

        return new OAuth2UserPrincipal(user, oAuth2User.getAttributes(), "sub");
    }

    @Transactional
    public void linkGoogleId(java.util.UUID userId, String googleId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setGoogleId(googleId);
            userRepository.save(u);
        });
    }
}
