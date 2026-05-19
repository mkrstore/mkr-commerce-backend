package com.mkr.commerce.user.dto;

import com.mkr.commerce.user.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
    @NotNull(message = "role is required")
    UserRole role
) {}
