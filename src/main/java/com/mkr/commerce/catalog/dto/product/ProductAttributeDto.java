package com.mkr.commerce.catalog.dto.product;

import com.mkr.commerce.catalog.entity.ProductAttribute;
import com.mkr.commerce.catalog.enums.FieldType;

import java.util.UUID;

public record ProductAttributeDto(
        UUID      id,
        UUID      definitionId,
        String    label,
        String    fieldKey,
        FieldType fieldType,
        String    unit,
        String    value
) {
    public static ProductAttributeDto from(ProductAttribute a) {
        return new ProductAttributeDto(
                a.getId(),
                a.getDefinition().getId(),
                a.getDefinition().getLabel(),
                a.getDefinition().getFieldKey(),
                a.getDefinition().getFieldType(),
                a.getDefinition().getUnit(),
                a.getValue()
        );
    }
}
