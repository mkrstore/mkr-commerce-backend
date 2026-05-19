package com.mkr.commerce.auth.dto;

import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.UserRole;

import java.util.UUID;

/**
 * Safe user payload returned to the frontend.
 * Never includes passwordHash or internal fields.
 */
public record AuthUserDto(
        UUID     id,
        String   name,
        String   firstName,
        String   lastName,
        String   email,
        UserRole role,
        String   department
) {
    public static AuthUserDto from(User user) {
        return new AuthUserDto(
                user.getId(),
                user.getName(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartment()
        );
    }
}
