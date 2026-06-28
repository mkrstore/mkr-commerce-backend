package com.mkr.commerce.inventory.dto;

import com.mkr.commerce.inventory.entity.InventoryTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InventoryTransactionDto(
        UUID       id,
        UUID       productId,
        String     productName,
        String     productSku,
        String     variantLabel,
        String     type,
        int        qty,
        BigDecimal purchasePricePerUnit,
        UUID       vendorId,
        String     vendorName,
        String     notes,
        String     createdByEmail,
        Instant    createdAt
) {
    public static InventoryTransactionDto from(InventoryTransaction tx) {
        String variantLabel = tx.getVariant() != null ? tx.getVariant().label() : null;
        return new InventoryTransactionDto(
                tx.getId(),
                tx.getProduct().getId(),
                tx.getProduct().getName(),
                tx.getProduct().getSku(),
                variantLabel,
                tx.getType().name(),
                tx.getQty(),
                tx.getPurchasePricePerUnit(),
                tx.getVendor() != null ? tx.getVendor().getId() : null,
                tx.getVendorName(),
                tx.getNotes(),
                tx.getCreatedByEmail(),
                tx.getCreatedAt()
        );
    }
}
