package com.mkr.commerce.user.dto;

import com.mkr.commerce.user.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for POST /api/users — create a new staff member.
 *
 * The company email is created by HR beforehand and provided here.
 * No password is set by the admin — the system sends an invitation
 * email to the staff member so they can set their own password.
 *
 * employeeId is auto-generated server-side (PostgreSQL sequence).
 */
public record CreateStaffRequest(

    // ── Name (mandatory) ──────────────────────────────────────────────────────

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must be under 50 characters")
    String firstName,

    @Size(max = 50, message = "Middle name must be under 50 characters")
    String middleName,                  // optional

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must be under 50 characters")
    String lastName,

    // ── Auth ──────────────────────────────────────────────────────────────────

    @NotBlank(message = "Company email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 150, message = "Email must be under 150 characters")
    String email,

    // ── Role & Organisation (mandatory) ───────────────────────────────────────

    @NotNull(message = "Role is required")
    UserRole role,

    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must be under 100 characters")
    String department,

    // ── Contact ───────────────────────────────────────────────────────────────

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Enter a valid mobile number")
    String mobileNumber,

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Enter a valid phone number")
    String alternativePhone,            // optional

    // ── Address (all optional) ────────────────────────────────────────────────

    @Size(max = 50)  String addressBuilding,
    @Size(max = 150) String addressStreet,
    @Size(max = 80)  String addressCity,
    @Size(max = 80)  String addressState,
    @Size(max = 20)  String addressPostalCode,
    @Size(max = 80)  String addressCountry

) {}
