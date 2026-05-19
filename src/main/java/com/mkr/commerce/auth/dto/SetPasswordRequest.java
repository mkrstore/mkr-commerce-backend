package com.mkr.commerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/auth/set-password.
 *
 * Used by newly invited staff to activate their account.
 * The invitation token is the only authentication — no login required.
 */
public record SetPasswordRequest(

    @NotBlank(message = "Invitation token is required")
    String token,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    String newPassword
) {}
