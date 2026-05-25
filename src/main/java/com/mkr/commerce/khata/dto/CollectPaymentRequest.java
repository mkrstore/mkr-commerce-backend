package com.mkr.commerce.khata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CollectPaymentRequest(
        @NotNull  @Positive BigDecimal amount,
        @NotBlank @Size(max = 50) String paymentMethod,
        @Size(max = 300) String note
) {}
