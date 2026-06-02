package com.mkr.commerce.customer.dto;

import com.mkr.commerce.customer.enums.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    String lastName,

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    String phone,

    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    String email,

    CustomerType type,

    @Size(max = 150) String addressStreet,
    @Size(max = 80)  String addressCity,
    @Size(max = 80)  String addressState,
    @Size(max = 20)  String addressPostalCode,
    @Size(max = 80)  String addressCountry
) {}
