package com.mkr.commerce.catalog.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
        @NotBlank @Size(max = 100) String  name,
        @Size(max = 100)           String  slug,
        String                         description,
        UUID                           parentId,
        int                            sortOrder,
        boolean                        isActive
) {}
