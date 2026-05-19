package com.mkr.commerce.auth.security;

import com.mkr.commerce.user.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collections;
import java.util.Map;

/**
 * Wraps our domain {@link User} inside the OAuth2 principal chain
 * so the success handler can retrieve it without an extra DB call.
 */
public class OAuth2UserPrincipal extends DefaultOAuth2User {

    private final User user;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes, String nameAttributeKey) {
        super(
            Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            ),
            attributes,
            nameAttributeKey
        );
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
