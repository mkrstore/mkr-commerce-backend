package com.mkr.commerce.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillLineItemRequest(
        @NotNull UUID productId,
        UUID         variantId,      // null for products without variants
        @Min(1)  int qty,
        @NotNull @DecimalMin("0") BigDecimal unitPrice,
        List<String> serialNumbers   // one per unit; null treated as empty
) {}
