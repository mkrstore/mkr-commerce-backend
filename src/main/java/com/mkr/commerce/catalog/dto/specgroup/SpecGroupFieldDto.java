package com.mkr.commerce.catalog.dto.specgroup;

import com.mkr.commerce.catalog.entity.SpecGroupField;

import java.util.UUID;

public record SpecGroupFieldDto(
        UUID   id,
        UUID   fieldId,
        String fieldName,
        String fieldType,
        int    sortOrder
) {
    public static SpecGroupFieldDto from(SpecGroupField sgf) {
        return new SpecGroupFieldDto(
                sgf.getId(),
                sgf.getField().getId(),
                sgf.getField().getName(),
                sgf.getField().getFieldType(),
                sgf.getSortOrder()
        );
    }
}
