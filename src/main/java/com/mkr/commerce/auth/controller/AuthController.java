package com.mkr.commerce.auth.controller;

import com.mkr.commerce.auth.dto.*;
import com.mkr.commerce.auth.service.AuthService;
import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.user.entity.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "mkr_refresh_token";

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    // ── POST /api/auth/login ──────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthTokenPair pair = authService.login(request);
        writeRefreshCookie(response, pair.rawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", pair.toLoginResponse()));
    }

    // ── POST /api/auth/refresh ────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            HttpServletRequest  request,
            HttpServletResponse response
    ) {
        String rawToken = readRefreshCookie(request);
        AuthTokenPair pair = authService.refresh(rawToken);
        writeRefreshCookie(response, pair.rawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", pair.toLoginResponse()));
    }

    // ── POST /api/auth/logout ─────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest  request,
            HttpServletResponse response
    ) {
        String rawToken = readRefreshCookie(request);
        authService.logout(rawToken);
        clearRefreshCookie(response);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    // ── GET /api/auth/me ─────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserDto>> me(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Current user", authService.getMe(user)));
    }

    // ── POST /api/auth/forgot-password ────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.forgotPassword(request);
        // Always return the same message — prevents user enumeration
        return ResponseEntity.ok(ApiResponse.ok(
                "If that email is registered, you'll receive a reset link within a few minutes."
        ));
    }

    // ── GET /api/auth/reset-password/validate ─────────────────────────────
    // Angular calls this when the user lands on /reset-password?token=xxx
    // to confirm the token is valid before showing the form.

    @GetMapping("/reset-password/validate")
    public ResponseEntity<ApiResponse<ValidateResetTokenResponse>> validateResetToken(
            @RequestParam String token
    ) {
        ValidateResetTokenResponse result = authService.validateResetToken(token);
        return ResponseEntity.ok(ApiResponse.ok("Token validated", result));
    }

    // ── POST /api/auth/reset-password ─────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successfully. Please log in with your new password."));
    }

    // ── GET /api/auth/invitation/validate ─────────────────────────────────
    // Angular calls this when staff lands on /set-password?token=xxx
    // to verify the token is still valid before showing the form.

    @GetMapping("/invitation/validate")
    public ResponseEntity<ApiResponse<ValidateResetTokenResponse>> validateInvitation(
            @RequestParam String token
    ) {
        ValidateResetTokenResponse result = authService.validateInvitation(token);
        return ResponseEntity.ok(ApiResponse.ok("Invitation validated", result));
    }

    // ── POST /api/auth/set-password ───────────────────────────────────────
    // Public endpoint — the invitation token is the only auth needed.
    // Sets the password and activates the account.

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordRequest request
    ) {
        authService.setPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Account activated. You can now log in with your company email and new password."));
    }

    // ── POST /api/auth/first-password ─────────────────────────────────────
    // Dev-env flow: new staff with pending invitation sets password without
    // the email link (used when email delivery is unavailable in dev).

    @PostMapping("/first-password")
    public ResponseEntity<ApiResponse<LoginResponse>> firstPassword(
            @Valid @RequestBody FirstPasswordRequest request,
            HttpServletResponse response
    ) {
        AuthTokenPair pair = authService.setFirstPassword(request);
        writeRefreshCookie(response, pair.rawRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Account activated. Welcome!", pair.toLoginResponse()));
    }

    // ── Cookie helpers ────────────────────────────────────────────────────

    private void writeRefreshCookie(HttpServletResponse response, String tokenValue) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, tokenValue);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setAttribute("SameSite", cookieSecure ? "None" : "Lax");
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
