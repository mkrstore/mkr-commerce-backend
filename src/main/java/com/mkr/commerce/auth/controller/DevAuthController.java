package com.mkr.commerce.auth.controller;

import com.mkr.commerce.auth.dto.AuthTokenPair;
import com.mkr.commerce.auth.dto.DirectResetRequest;
import com.mkr.commerce.auth.dto.LoginResponse;
import com.mkr.commerce.auth.service.AuthService;
import com.mkr.commerce.common.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/api/auth/dev")
@RequiredArgsConstructor
public class DevAuthController {

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    private final AuthService authService;

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<LoginResponse>> devResetPassword(
            @Valid @RequestBody DirectResetRequest request,
            HttpServletResponse response
    ) {
        AuthTokenPair pair = authService.directResetPassword(request);
        Cookie cookie = new Cookie("mkr_refresh_token", pair.rawRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshTokenExpiryMs / 1000));
        response.addCookie(cookie);
        return ResponseEntity.ok(ApiResponse.ok("Password reset. Welcome back!", pair.toLoginResponse()));
    }
}
