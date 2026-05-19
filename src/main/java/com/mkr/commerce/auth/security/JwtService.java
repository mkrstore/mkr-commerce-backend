package com.mkr.commerce.auth.security;

import com.mkr.commerce.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * All JWT operations in one place:
 *   - generate access token
 *   - validate and parse claims
 *
 * Access tokens are short-lived (15 min by default).
 * They carry: sub (userId), email, role.
 *
 * Refresh token generation is just a UUID — it is stored
 * in the DB by AuthService (not a signed JWT).
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey  key;
    private final long       accessTokenExpiryMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-ms}") long accessTokenExpiryMs
    ) {
        this.key                 = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
    }

    // ── Token generation ─────────────────────────────────────────────────

    public String generateAccessToken(User user) {
        Instant now    = Instant.now();
        Instant expiry = now.plusMillis(accessTokenExpiryMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role",  user.getRole().name())
                .claim("name",  user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /** Raw UUID — not a JWT. Stored in DB, sent as HttpOnly cookie. */
    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    // ── Token validation & extraction ────────────────────────────────────

    /**
     * Returns true if token is valid (not expired, not tampered).
     * Throws {@link ExpiredJwtException} for expired tokens so callers
     * can distinguish expiry from a genuinely invalid token.
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            throw ex;  // propagate — JwtAuthFilter catches this specifically
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
