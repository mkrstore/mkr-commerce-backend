package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.enums.MediaType;
import com.mkr.commerce.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductSummaryDto(
        UUID                    id,
        String                  name,
        String                  slug,
        String                  sku,
        String                  barcode,
        String                  categoryName,
        String                  brandName,
        BigDecimal              priceRetail,
        BigDecimal              priceWholesale,
        BigDecimal              priceBroker,
        BigDecimal              gstPercent,
        int                     stockQty,
        ProductStatus           status,
        String                  primaryImageUrl,
        Instant                 createdAt,
        List<ProductVariantDto> variants
) {
    public static ProductSummaryDto from(Product p) {
        String primaryImg = p.getImages().stream()
                .filter(i -> i.getMediaType() == MediaType.IMAGE_PRIMARY)
                .findFirst()
                .or(() -> p.getImages().stream().findFirst())
                .map(i -> i.getUrl()).orElse(null);

        List<ProductVariantDto> variants = p.getVariants().stream()
                .filter(v -> v.isActive())
                .map(ProductVariantDto::from)
                .toList();

        return new ProductSummaryDto(
                p.getId(), p.getName(), p.getSlug(), p.getSku(), p.getBarcode(),
                p.getCategory().getName(),
                p.getBrand() != null ? p.getBrand().getName() : null,
                p.getPriceRetail(), p.getPriceWholesale(), p.getPriceBroker(),
                p.getGstPercent(), p.getStockQty(),
                p.getStatus(), primaryImg, p.getCreatedAt(),
                variants
        );
    }
}
