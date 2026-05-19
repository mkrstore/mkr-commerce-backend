package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.enums.MediaType;
import com.mkr.commerce.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductSummaryDto(
        UUID          id,
        String        name,
        String        slug,
        String        sku,
        String        categoryName,
        String        brandName,
        BigDecimal    priceRetail,
        BigDecimal    gstPercent,
        int           stockQty,
        ProductStatus status,
        String        primaryImageUrl,
        Instant       createdAt
) {
    public static ProductSummaryDto from(Product p) {
        String primaryImg = p.getImages().stream()
                .filter(i -> i.getMediaType() == MediaType.IMAGE_PRIMARY)
                .findFirst()
                .or(() -> p.getImages().stream().findFirst())
                .map(i -> i.getUrl()).orElse(null);

        return new ProductSummaryDto(
                p.getId(), p.getName(), p.getSlug(), p.getSku(),
                p.getCategory().getName(),
                p.getBrand() != null ? p.getBrand().getName() : null,
                p.getPriceRetail(), p.getGstPercent(), p.getStockQty(),
                p.getStatus(), primaryImg, p.getCreatedAt()
        );
    }
}
