package com.mkr.commerce.auth.dto;

/**
 * Returned by GET /api/auth/reset-password/validate?token=xxx
 * Angular uses this to show the reset form or an error page
 * before the user even types a new password.
 */
public record ValidateResetTokenResponse(
        boolean valid,
        String  maskedEmail,
        String  message
) {}
