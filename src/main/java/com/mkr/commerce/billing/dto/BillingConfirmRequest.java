package com.mkr.commerce.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * POS billing confirm request.
 *
 * Customer resolution order:
 *   1. customerId (UUID) — use existing known customer
 *   2. customerPhone — search by phone; create new RETAIL customer if not found
 *   3. Walk-in (no phone) — create a minimal customer record
 *
 * khataAmount = 0           → full payment collected now
 * khataAmount = grandTotal  → full khata (take now pay later)
 * 0 < khataAmount < total   → partial: paidViaMethod collected, rest to khata
 */
public record BillingConfirmRequest(

        // ── Customer ─────────────────────────────────────────────────────────
        UUID   customerId,
        String customerPhone,
        String customerName,
        String customerEmail,
        String customerAddress,

        // ── Items ─────────────────────────────────────────────────────────────
        @NotEmpty @Valid List<BillLineItemRequest> items,

        // ── Bill config ───────────────────────────────────────────────────────
        boolean gstEnabled,

        // ── Payment ───────────────────────────────────────────────────────────
        @NotBlank String paymentMethod,

        @NotNull @DecimalMin("0") BigDecimal khataAmount,

        // cash/upi/card — the method used for the portion collected now
        // relevant only when paymentMethod == "partial"
        String paidViaMethod
) {}
