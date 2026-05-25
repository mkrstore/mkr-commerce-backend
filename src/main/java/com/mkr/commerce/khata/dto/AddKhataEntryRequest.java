package com.mkr.commerce.khata.dto;

import com.mkr.commerce.khata.enums.KhataEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AddKhataEntryRequest(
        @NotNull  KhataEntryType entryType,
        @NotBlank @Size(max = 250) String description,
        @NotNull  @Positive BigDecimal amount,
        @Size(max = 300) String notes
) {}
