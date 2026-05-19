package com.mkr.commerce.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * PATCH /api/users/{id} — all fields are optional.
 * null  = "don't touch this field"
 * ""    = "clear this field" (only valid for optional fields like middleName, alternativePhone, address)
 */
public record UpdateStaffRequest(

    @Size(max = 50) String firstName,
    @Size(max = 50) String middleName,
    @Size(max = 50) String lastName,

    @Email @Size(max = 150) String email,

    @Size(max = 100) String department,

    @Size(max = 20) String mobileNumber,
    @Size(max = 20) String alternativePhone,

    @Size(max = 50)  String addressBuilding,
    @Size(max = 150) String addressStreet,
    @Size(max = 80)  String addressCity,
    @Size(max = 80)  String addressState,
    @Size(max = 20)  String addressPostalCode,
    @Size(max = 80)  String addressCountry

) {}
