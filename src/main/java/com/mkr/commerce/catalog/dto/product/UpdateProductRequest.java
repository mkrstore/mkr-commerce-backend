package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.enums.ProductStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * PATCH-style update — all fields optional, service only applies non-null values.
 */
public record UpdateProductRequest(

        @Size(max = 200) String        name,
        @Size(max = 200) String        slug,
        @Size(max = 80)  String        sku,
        @Size(max = 500) String        shortDescription,
                         String        description,
        @Size(max = 50)  String        barcode,
                         UUID          categoryId,
                         UUID          brandId,
                         UUID          preferredVendorId,

        @DecimalMin("0.01") BigDecimal priceRetail,
        @DecimalMin("0.01") BigDecimal priceWholesale,
        @DecimalMin("0.01") BigDecimal priceBroker,
        @Min(1)             Integer    minQtyWholesale,

        @DecimalMin("0") @DecimalMax("100") BigDecimal gstPercent,
        Boolean          gstIncluded,

        @Min(0) Integer  stockQty,
        BigDecimal       weight,
        @Size(max = 50)  String dimensions,

        ProductStatus    status,
        Set<String>      tags
) {}
