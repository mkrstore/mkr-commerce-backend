package com.mkr.commerce.catalog.dto.lookup;

import com.mkr.commerce.catalog.entity.LookupList;

import java.util.UUID;

public record LookupListSummaryDto(
        UUID   id,
        String name,
        String description,
        String fieldType,
        boolean required,
        String defaultValue,
        int    valueCount
) {
    public static LookupListSummaryDto from(LookupList l) {
        return new LookupListSummaryDto(
                l.getId(),
                l.getName(),
                l.getDescription(),
                l.getFieldType(),
                l.isRequired(),
                l.getDefaultValue(),
                l.getValues().size()
        );
    }
}
