package com.mkr.commerce.catalog.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateVariantRequest(
        @NotBlank @Size(max = 100) String     sku,
        @Size(max = 80)            String     colorName,
        @Size(max = 10)            String     colorHex,
        @Size(max = 40)            String     size,
        @DecimalMin("0.01")        BigDecimal priceOverride,
        @Min(0)                    int        stockQty
) {}
