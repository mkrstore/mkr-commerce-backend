package com.mkr.commerce.customer.dto;

import com.mkr.commerce.customer.enums.CustomerType;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomerTypeRequest(
        @NotNull(message = "Customer type is required")
        CustomerType type
) {}
