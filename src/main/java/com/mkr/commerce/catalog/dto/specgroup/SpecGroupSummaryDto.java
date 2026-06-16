package com.mkr.commerce.catalog.dto.specgroup;

import com.mkr.commerce.catalog.entity.SpecGroup;

import java.util.UUID;

public record SpecGroupSummaryDto(
        UUID   id,
        String name,
        String description,
        int    fieldCount
) {
    public static SpecGroupSummaryDto from(SpecGroup g) {
        return new SpecGroupSummaryDto(
                g.getId(), g.getName(), g.getDescription(), g.getFields().size()
        );
    }
}
