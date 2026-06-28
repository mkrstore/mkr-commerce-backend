package com.mkr.commerce.inventory.dto;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.UUID;

public record RestockRequest(
        UUID       variantId,
        @Min(1) int qty,
        UUID       vendorId,              // links to a Vendor record (preferred)
        String     vendorName,            // fallback free-text when no vendorId
        BigDecimal purchasePricePerUnit,
        String     notes
) {}
