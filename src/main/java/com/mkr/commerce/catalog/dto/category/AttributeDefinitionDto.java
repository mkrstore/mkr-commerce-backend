package com.mkr.commerce.catalog.dto.category;

import com.mkr.commerce.catalog.entity.AttributeDefinition;
import com.mkr.commerce.catalog.enums.FieldType;

import java.util.UUID;

public record AttributeDefinitionDto(
        UUID      id,
        String    label,
        String    fieldKey,
        FieldType fieldType,
        String    options,
        String    unit,
        String    defaultValue,
        boolean   required,
        int       sortOrder,
        String    groupName
) {
    public static AttributeDefinitionDto from(AttributeDefinition a) {
        return new AttributeDefinitionDto(
                a.getId(), a.getLabel(), a.getFieldKey(), a.getFieldType(),
                a.getOptions(), a.getUnit(), a.getDefaultValue(),
                a.isRequired(), a.getSortOrder(), a.getGroupName()
        );
    }
}
