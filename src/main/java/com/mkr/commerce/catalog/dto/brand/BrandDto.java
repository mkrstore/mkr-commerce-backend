package com.mkr.commerce.catalog.dto.brand;

import com.mkr.commerce.catalog.entity.Brand;

import java.time.Instant;
import java.util.UUID;

public record BrandDto(
        UUID    id,
        String  name,
        String  slug,
        String  logoUrl,
        boolean logoIsVideo,
        String  description,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static BrandDto from(Brand b) {
        return new BrandDto(
                b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(), b.isLogoIsVideo(),
                b.getDescription(), b.isActive(), b.getCreatedAt(), b.getUpdatedAt()
        );
    }
}
