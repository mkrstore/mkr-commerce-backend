package com.mkr.commerce.auth.dto;

/**
 * Response body for POST /api/auth/login and POST /api/auth/refresh.
 *
 * accessToken  — short-lived JWT, stored in Angular memory (signal).
 * user         — profile used to populate sidebar / topbar.
 *
 * refreshToken is NOT in the body; it is set as an HttpOnly cookie
 * by AuthController so JavaScript cannot access it.
 */
public record LoginResponse(
        String      accessToken,
        AuthUserDto user
) {}
