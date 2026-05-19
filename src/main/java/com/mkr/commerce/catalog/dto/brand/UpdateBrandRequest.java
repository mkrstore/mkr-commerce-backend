package com.mkr.commerce.catalog.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateBrandRequest(
        @NotBlank @Size(max = 100) String  name,
        @Size(max = 100)           String  slug,
        String                         description,
        boolean                        isActive
) {}
