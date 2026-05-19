package com.mkr.commerce.catalog.dto.category;

import com.mkr.commerce.catalog.entity.Category;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CategoryTreeDto(
        UUID                  id,
        String                name,
        String                slug,
        String                imageUrl,
        int                   sortOrder,
        List<CategoryTreeDto> children
) {
    public static CategoryTreeDto from(Category c) {
        List<CategoryTreeDto> kids = c.getChildren().stream()
                .filter(Category::isActive)
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .map(CategoryTreeDto::from)
                .toList();
        return new CategoryTreeDto(c.getId(), c.getName(), c.getSlug(), c.getImageUrl(), c.getSortOrder(), kids);
    }
}
