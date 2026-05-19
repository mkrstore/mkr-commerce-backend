package com.mkr.commerce.user.dto;

import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.UserRole;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe staff member payload returned to the frontend.
 * Never includes passwordHash, googleId, or internal DB fields.
 */
public record StaffDto(

    // Identity
    UUID     id,
    Long     employeeId,

    // Name
    String   firstName,
    String   middleName,
    String   lastName,
    String   name,              // full name (composed)

    // Auth
    String   email,
    UserRole role,
    String   department,

    // Status
    boolean  isActive,
    boolean  invitationPending, // true = account created but password not yet set

    // Contact
    String   mobileNumber,
    String   alternativePhone,

    // Address
    String   addressBuilding,
    String   addressStreet,
    String   addressCity,
    String   addressState,
    String   addressPostalCode,
    String   addressCountry,

    // Audit
    Instant  createdAt

) {
    public static StaffDto from(User user) {
        return new StaffDto(
            user.getId(),
            user.getEmployeeId(),
            user.getFirstName(),
            user.getMiddleName(),
            user.getLastName(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getDepartment(),
            user.isActive(),
            user.getPasswordHash() == null && user.getGoogleId() == null,
            user.getMobileNumber(),
            user.getAlternativePhone(),
            user.getAddressBuilding(),
            user.getAddressStreet(),
            user.getAddressCity(),
            user.getAddressState(),
            user.getAddressPostalCode(),
            user.getAddressCountry(),
            user.getCreatedAt()
        );
    }
}
