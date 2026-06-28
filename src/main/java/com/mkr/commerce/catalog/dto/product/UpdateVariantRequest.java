package com.mkr.commerce.catalog.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateVariantRequest(
        @NotBlank @Size(max = 100) String              sku,
        Map<String, String>        attributes,
        @DecimalMin("0.01")        BigDecimal          priceOverride,
        BigDecimal                 priceWholesale,
        BigDecimal                 priceBroker,
        @Min(0)                    int                 stockQty,
        boolean                    isActive
) {}
