package com.mkr.commerce.billing.dto;

import com.mkr.commerce.billing.entity.Bill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

public record CustomerBillSummaryDto(
        UUID       id,
        String     billId,
        String     paymentMethod,
        BigDecimal grandTotal,
        BigDecimal paidNow,
        BigDecimal khataAmount,
        boolean    gstEnabled,
        String     itemsSummary,
        int        itemCount,
        Instant    createdAt
) {
    public static CustomerBillSummaryDto from(Bill b) {
        String summary = b.getLineItems().stream()
                .limit(3)
                .map(li -> li.getProductName() + " ×" + li.getQty())
                .collect(Collectors.joining(", "));
        if (b.getLineItems().size() > 3) summary += " …";

        return new CustomerBillSummaryDto(
                b.getId(),
                b.getBillId(),
                b.getPaymentMethod(),
                b.getGrandTotal(),
                b.getPaidNow(),
                b.getKhataAmount(),
                b.isGstEnabled(),
                summary,
                b.getLineItems().size(),
                b.getCreatedAt()
        );
    }
}
