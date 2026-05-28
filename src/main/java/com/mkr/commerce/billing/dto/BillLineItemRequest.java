package com.mkr.commerce.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BillLineItemRequest(
        @NotNull UUID productId,
        @Min(1)  int qty,
        @NotNull @DecimalMin("0") BigDecimal unitPrice,
        BigDecimal discount       // null → treated as 0
) {}
