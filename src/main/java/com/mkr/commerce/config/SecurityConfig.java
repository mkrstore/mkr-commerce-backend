package com.mkr.commerce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkr.commerce.auth.security.JwtAuthFilter;
import com.mkr.commerce.auth.security.OAuth2FailureHandler;
import com.mkr.commerce.auth.security.OAuth2SuccessHandler;
import com.mkr.commerce.auth.security.CookieOAuth2AuthorizationRequestRepository;
import com.mkr.commerce.auth.service.CustomOAuth2UserService;
import com.mkr.commerce.auth.service.CustomOidcUserService;
import com.mkr.commerce.common.exception.ErrorCode;
import com.mkr.commerce.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter           jwtAuthFilter;
    private final UserDetailsService       userDetailsService;
    private final ObjectMapper             objectMapper;
    private final CustomOAuth2UserService                    customOAuth2UserService;
    private final CustomOidcUserService                      customOidcUserService;
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository;
    private final OAuth2SuccessHandler     oauth2SuccessHandler;
    private final OAuth2FailureHandler     oauth2FailureHandler;
    private final PasswordEncoder          passwordEncoder;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // ── Security filter chain ─────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // IF_REQUIRED: Spring only creates a session when needed (OAuth2 state exchange).
            // The session is invalidated immediately after by OAuth2SuccessHandler.
            // All API endpoints are stateless — authenticated via JWT Bearer only.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/me").authenticated()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            // Replace Spring's default 401 HTML with our ApiResponse JSON
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestRepository(cookieAuthRequestRepository)
                )
                .userInfoEndpoint(ui -> ui
                    .userService(customOAuth2UserService)
                    .oidcUserService(customOidcUserService)
                )
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler)
            );

        return http.build();
    }

    // ── Custom entry point — returns ApiResponse JSON with correct errorCode ──

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            // JwtAuthFilter sets this attribute for expired/invalid tokens
            String attrCode = (String) request.getAttribute(JwtAuthFilter.JWT_ERROR_CODE_ATTR);

            ErrorCode errorCode;
            String    message;

            if ("ACCESS_TOKEN_EXPIRED".equals(attrCode)) {
                errorCode = ErrorCode.ACCESS_TOKEN_EXPIRED;
                message   = "Access token has expired. Please refresh your session.";
            } else if ("ACCOUNT_DEACTIVATED".equals(attrCode)) {
                errorCode = ErrorCode.ACCOUNT_DEACTIVATED;
                message   = "Your account has been deactivated.";
            } else {
                errorCode = ErrorCode.ACCESS_TOKEN_INVALID;
                message   = "Authentication required. Please log in.";
            }

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(message, errorCode));
        };
    }

    // ── CORS ──────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // required for HttpOnly cookie (refresh token)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // ── Auth provider ─────────────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

}
