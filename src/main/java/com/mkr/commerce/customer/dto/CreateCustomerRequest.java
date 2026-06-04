package com.mkr.commerce.customer.dto;

import com.mkr.commerce.customer.enums.CustomerType;
import jakarta.validation.constraints.*;

public record CreateCustomerRequest(

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    String lastName,

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    String phone,

    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    String email,

    CustomerType type,

    @Size(max = 150) String addressStreet,
    @Size(max = 80)  String addressCity,
    @Size(max = 80)  String addressMandal,
    @Size(max = 80)  String addressDistrict,
    @Size(max = 80)  String addressState,
    @Size(max = 20)  String addressPostalCode
) {}
