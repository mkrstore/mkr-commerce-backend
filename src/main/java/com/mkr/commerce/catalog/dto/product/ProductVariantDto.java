package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductVariantDto(
        UUID                id,
        String              sku,
        Map<String, String> attributes,
        String              label,
        BigDecimal          priceOverride,
        BigDecimal          priceWholesale,
        BigDecimal          priceBroker,
        int                 stockQty,
        boolean             isActive
) {
    public static ProductVariantDto from(ProductVariant v) {
        return new ProductVariantDto(
                v.getId(),
                v.getSku(),
                v.getAttributes(),
                v.label(),
                v.getPriceOverride(),
                v.getPriceWholesale(),
                v.getPriceBroker(),
                v.getStockQty(),
                v.isActive()
        );
    }
}
