package com.mkr.commerce.common.exception;

/**
 * Machine-readable error codes sent in every error response.
 *
 * Frontend switches on these — never on HTTP status or message strings.
 * Adding a new code here is a one-place change; message wording can change
 * freely without breaking frontend logic.
 */
public enum ErrorCode {

    // ── Auth ──────────────────────────────────────────────────────────────
    ACCESS_TOKEN_EXPIRED,     // JWT signature valid but token has expired
    ACCESS_TOKEN_INVALID,     // JWT malformed, wrong signature, or missing
    NO_SESSION,               // No refresh token cookie at all — not a prior session
    REFRESH_TOKEN_INVALID,    // Refresh token not found, revoked, or expired
    INVALID_CREDENTIALS,      // Wrong email or password
    ACCOUNT_DEACTIVATED,      // User exists but isActive = false
    GOOGLE_LOGIN_REQUIRED,    // Account has no password — must use Google OAuth

    // ── Password reset ────────────────────────────────────────────────────
    RESET_TOKEN_INVALID,      // Reset token not found or already used
    RESET_TOKEN_EXPIRED,      // Reset token found but past its expiry time
    PASSWORD_RECENTLY_USED,   // New password matches one of the last N passwords
    CAPTCHA_FAILED,           // reCAPTCHA verification returned success=false

    // ── Validation ────────────────────────────────────────────────────────
    VALIDATION_ERROR,         // Bean Validation (@Valid) failure

    // ── Resource ──────────────────────────────────────────────────────────
    RESOURCE_NOT_FOUND,       // Requested entity does not exist
    FORBIDDEN,                // Authenticated but not authorised for this action

    // ── Invitation (new staff onboarding) ────────────────────────────────
    INVITATION_TOKEN_INVALID, // Token not found or already used
    INVITATION_TOKEN_EXPIRED, // Token found but past its expiry time
    ROLE_NOT_PERMITTED,       // Creator cannot assign a role at or above their own level

    // ── Business rules ────────────────────────────────────────────────────
    INSUFFICIENT_STOCK,       // Product stock < requested quantity
    DUPLICATE_EMAIL,          // Email already registered
    DUPLICATE_PHONE,          // Phone already registered
    INVALID_PROMO_CODE,       // Promo code not found, expired, or over usage limit

    // ── Generic ───────────────────────────────────────────────────────────
    INTERNAL_ERROR,           // Unexpected server-side failure
}
