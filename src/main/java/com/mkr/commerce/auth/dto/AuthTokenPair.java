package com.mkr.commerce.auth.dto;

/**
 * Internal transport between AuthService and AuthController.
 *
 * accessToken    — short-lived JWT → returned in response body.
 * rawRefreshToken — UUID string   → written as HttpOnly cookie by controller.
 * user           — profile data   → returned in response body.
 *
 * This record is never serialised to JSON directly.
 */
public record AuthTokenPair(
        String      accessToken,
        String      rawRefreshToken,
        AuthUserDto user
) {
    /** Build the body-safe login response (no refresh token). */
    public LoginResponse toLoginResponse() {
        return new LoginResponse(accessToken, user);
    }
}
