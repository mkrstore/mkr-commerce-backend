package com.mkr.commerce.billing.dto;

import com.mkr.commerce.billing.entity.BillLineItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillLineItemResponse(
        UUID       productId,
        String     productName,
        String     productSku,
        int        qty,
        BigDecimal unitPrice,
        BigDecimal discount,
        BigDecimal gstPercent,
        BigDecimal lineTotal,
        BigDecimal gstAmount,
        List<String> serialNumbers
) {
    public static BillLineItemResponse from(BillLineItem li) {
        return new BillLineItemResponse(
                li.getProduct() != null ? li.getProduct().getId() : null,
                li.getProductName(),
                li.getProductSku(),
                li.getQty(),
                li.getUnitPrice(),
                li.getDiscount(),
                li.getGstPercent(),
                li.getLineTotal(),
                li.getGstAmount(),
                li.getSerialNumbers() != null ? li.getSerialNumbers() : List.of()
        );
    }
}
