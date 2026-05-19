package com.mkr.commerce.user.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

/**
 * Staff member who can log into the admin portal.
 *
 * Name is stored in three parts (first / middle / last) and composed
 * into a full name on save.  The JWT and sidebar always use fullName.
 *
 * employeeId is assigned from a PostgreSQL sequence (employee_id_seq)
 * so it is always unique, always incrementing, and never editable.
 *
 * passwordHash  — null for Google-only accounts.
 * googleId      — null for email/password-only accounts.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email",       columnList = "email",       unique = true),
        @Index(name = "idx_users_employee_id", columnList = "employee_id", unique = true),
        @Index(name = "idx_users_google_id",   columnList = "google_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Auto-incrementing staff number (1001, 1002, …). Never editable. */
    @Column(name = "employee_id", unique = true, updatable = false)
    private Long employeeId;

    // ── Name ──────────────────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;          // optional

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /** Composed full name — stored so JWT claims and search work without joins. */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id", length = 100)
    private String googleId;

    // ── Role & Organisation ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(length = 100)
    private String department;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── Contact ───────────────────────────────────────────────────────────────

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "alternative_phone", length = 20)
    private String alternativePhone;   // optional

    // ── Address (all optional) ────────────────────────────────────────────────

    @Column(name = "addr_building", length = 50)
    private String addressBuilding;

    @Column(name = "addr_street", length = 150)
    private String addressStreet;

    @Column(name = "addr_city", length = 80)
    private String addressCity;

    @Column(name = "addr_state", length = 80)
    private String addressState;

    @Column(name = "addr_postal_code", length = 20)
    private String addressPostalCode;

    @Column(name = "addr_country", length = 80)
    private String addressCountry;

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Compose and store the full name from its parts.
     * Call this whenever firstName / middleName / lastName changes.
     */
    public void composeName() {
        StringBuilder sb = new StringBuilder(firstName.trim());
        if (middleName != null && !middleName.isBlank()) {
            sb.append(' ').append(middleName.trim());
        }
        sb.append(' ').append(lastName.trim());
        this.name = sb.toString();
    }
}
