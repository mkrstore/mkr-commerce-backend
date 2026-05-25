package com.mkr.commerce.khata.dto;

import com.mkr.commerce.khata.entity.KhataEntry;
import com.mkr.commerce.khata.enums.KhataEntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record KhataEntryDto(
        UUID           id,
        KhataEntryType entryType,
        String         description,
        BigDecimal     debit,
        BigDecimal     credit,
        BigDecimal     balance,
        String         orderId,
        String         paymentMethod,
        String         notes,
        Instant        createdAt
) {
    public static KhataEntryDto from(KhataEntry e) {
        return new KhataEntryDto(
                e.getId(),
                e.getEntryType(),
                e.getDescription(),
                e.getDebit(),
                e.getCredit(),
                e.getBalance(),
                e.getOrderId(),
                e.getPaymentMethod(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }
}
