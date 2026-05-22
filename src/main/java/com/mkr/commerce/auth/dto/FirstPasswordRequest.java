package com.mkr.commerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/auth/first-password.
 *
 * Dev-env flow for new staff who can't receive invitation emails:
 * if an account exists with no password set and a pending invitation,
 * this endpoint allows direct password setup without the email token.
 */
public record FirstPasswordRequest(

    @NotBlank(message = "Email or phone is required")
    String identifier,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be 8–128 characters")
    String newPassword
) {}
