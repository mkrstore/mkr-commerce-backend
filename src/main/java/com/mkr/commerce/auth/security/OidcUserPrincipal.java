package com.mkr.commerce.auth.security;

import com.mkr.commerce.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Wraps our domain {@link User} inside the OIDC principal chain.
 * Used when Google authenticates via OIDC (openid scope) instead of plain OAuth2.
 */
public class OidcUserPrincipal implements OidcUser {

    private final User     user;
    private final OidcUser delegate;

    public OidcUserPrincipal(User user, OidcUser delegate) {
        this.user     = user;
        this.delegate = delegate;
    }

    public User getUser() { return user; }

    @Override public Map<String, Object> getClaims()     { return delegate.getClaims(); }
    @Override public OidcUserInfo        getUserInfo()   { return delegate.getUserInfo(); }
    @Override public OidcIdToken         getIdToken()    { return delegate.getIdToken(); }
    @Override public Map<String, Object> getAttributes() { return delegate.getAttributes(); }
    @Override public String              getName()       { return delegate.getName(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }
}
