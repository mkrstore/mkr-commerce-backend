package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductDetailDto(
        UUID                      id,
        String                    name,
        String                    slug,
        String                    sku,
        String                    barcode,
        String                    shortDescription,
        String                    description,
        UUID                      categoryId,
        String                    categoryName,
        UUID                      brandId,
        String                    brandName,
        BigDecimal                priceRetail,
        BigDecimal                priceWholesale,
        BigDecimal                priceBroker,
        int                       minQtyWholesale,
        BigDecimal                gstPercent,
        boolean                   gstIncluded,
        int                       stockQty,
        BigDecimal                weight,
        String                    dimensions,
        ProductStatus             status,
        List<ProductImageDto>     media,
        List<ProductVariantDto>   variants,
        List<ProductAttributeDto> attributes,
        Set<String>               tags,
        Instant                   createdAt,
        Instant                   updatedAt
) {
    public static ProductDetailDto from(Product p) {
        return new ProductDetailDto(
                p.getId(), p.getName(), p.getSlug(), p.getSku(),
                p.getBarcode(), p.getShortDescription(), p.getDescription(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getBrand() != null ? p.getBrand().getId()   : null,
                p.getBrand() != null ? p.getBrand().getName() : null,
                p.getPriceRetail(), p.getPriceWholesale(), p.getPriceBroker(),
                p.getMinQtyWholesale(), p.getGstPercent(), p.isGstIncluded(),
                p.getStockQty(), p.getWeight(), p.getDimensions(), p.getStatus(),
                p.getImages().stream().map(ProductImageDto::from).toList(),
                p.getVariants().stream().map(ProductVariantDto::from).toList(),
                p.getAttributes().stream().map(ProductAttributeDto::from).toList(),
                p.getTags().stream().map(t -> t.getName()).collect(Collectors.toSet()),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
