package com.mkr.commerce.catalog.dto.category;

import com.mkr.commerce.catalog.entity.Category;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CategoryDto(
        UUID                        id,
        String                      name,
        String                      slug,
        String                      description,
        String                      imageUrl,
        UUID                        parentId,
        String                      parentName,
        int                         sortOrder,
        boolean                     isActive,
        List<AttributeDefinitionDto> attributeDefinitions,
        Instant                     createdAt,
        Instant                     updatedAt
) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(
                c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
                c.getParent() != null ? c.getParent().getId()   : null,
                c.getParent() != null ? c.getParent().getName() : null,
                c.getSortOrder(), c.isActive(),
                c.getAttributeDefinitions().stream().map(AttributeDefinitionDto::from).toList(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
