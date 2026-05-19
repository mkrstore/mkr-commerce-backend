package com.mkr.commerce.catalog.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Step 1 of the product wizard — minimum fields to create a Draft.
 * Pricing, attributes, and media are added via separate PATCH / upload calls.
 */
public record CreateProductRequest(

        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 80)  String sku,
        @NotNull                   UUID   categoryId,

        UUID                             brandId,
        @Size(max = 500)           String shortDescription,
                                   String description,
        @Size(max = 200)           String slug,
        @Size(max = 50)            String barcode
) {}
