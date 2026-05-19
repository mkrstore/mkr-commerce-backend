package com.mkr.commerce.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
    @NotNull(message = "active flag is required")
    Boolean active
) {}
