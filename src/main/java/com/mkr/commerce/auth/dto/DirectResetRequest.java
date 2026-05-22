package com.mkr.commerce.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DirectResetRequest(
        @NotBlank(message = "Email or phone is required") String identifier,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be 8–128 characters") String newPassword
) {}
