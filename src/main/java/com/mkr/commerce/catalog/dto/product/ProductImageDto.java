package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.ProductImage;
import com.mkr.commerce.catalog.enums.MediaType;

import java.util.UUID;

public record ProductImageDto(
        UUID      id,
        MediaType mediaType,
        String    url,
        String    altText,
        int       sortOrder
) {
    public static ProductImageDto from(ProductImage i) {
        return new ProductImageDto(i.getId(), i.getMediaType(), i.getUrl(), i.getAltText(), i.getSortOrder());
    }
}
