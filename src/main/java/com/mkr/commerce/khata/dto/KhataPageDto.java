package com.mkr.commerce.khata.dto;

import java.math.BigDecimal;
import java.util.List;

public record KhataPageDto(
        List<KhataEntryDto> entries,
        BigDecimal          currentBalance
) {}
