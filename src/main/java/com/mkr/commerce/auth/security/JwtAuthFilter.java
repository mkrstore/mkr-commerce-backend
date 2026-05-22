package com.mkr.commerce.auth.security;

import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Validates the Bearer token on every protected request.
 *
 * Sets a request attribute "JWT_ERROR_CODE" so the AuthenticationEntryPoint
 * can return the correct errorCode (ACCESS_TOKEN_EXPIRED vs ACCESS_TOKEN_INVALID)
 * instead of a generic 401.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    /** Request attribute key — read by AuthEntryPoint to set errorCode. */
    public static final String JWT_ERROR_CODE_ATTR = "JWT_ERROR_CODE";

    private final JwtService     jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            if (!jwtService.isValid(token)) {
                request.setAttribute(JWT_ERROR_CODE_ATTR, "ACCESS_TOKEN_INVALID");
                chain.doFilter(request, response);
                return;
            }
        } catch (ExpiredJwtException ex) {
            // Token signature is valid but it has expired — frontend should refresh
            request.setAttribute(JWT_ERROR_CODE_ATTR, "ACCESS_TOKEN_EXPIRED");
            chain.doFilter(request, response);
            return;
        } catch (JwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            request.setAttribute(JWT_ERROR_CODE_ATTR, "ACCESS_TOKEN_INVALID");
            chain.doFilter(request, response);
            return;
        }

        // Already authenticated in this request
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String userIdStr = jwtService.extractUserId(token);
        String role      = jwtService.extractRole(token);

        userRepository.findById(UUID.fromString(userIdStr)).ifPresent(user -> {
            if (!user.isActive()) {
                log.warn("Blocked inactive user: {}", user.getEmail());
                request.setAttribute(JWT_ERROR_CODE_ATTR, "ACCOUNT_DEACTIVATED");
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        });

        chain.doFilter(request, response);
    }
}
