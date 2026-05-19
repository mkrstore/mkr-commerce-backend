package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantDto(
        UUID       id,
        String     sku,
        String     colorName,
        String     colorHex,
        String     size,
        BigDecimal priceOverride,
        int        stockQty,
        boolean    isActive
) {
    public static ProductVariantDto from(ProductVariant v) {
        return new ProductVariantDto(
                v.getId(), v.getSku(), v.getColorName(), v.getColorHex(),
                v.getSize(), v.getPriceOverride(), v.getStockQty(), v.isActive()
        );
    }
}
