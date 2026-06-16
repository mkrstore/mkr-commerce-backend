package com.mkr.commerce.catalog.dto.specgroup;

import com.mkr.commerce.catalog.entity.SpecGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SpecGroupDto(
        UUID                   id,
        String                 name,
        String                 description,
        List<SpecGroupFieldDto> fields,
        Instant                createdAt,
        Instant                updatedAt
) {
    public static SpecGroupDto from(SpecGroup g) {
        return new SpecGroupDto(
                g.getId(),
                g.getName(),
                g.getDescription(),
                g.getFields().stream().map(SpecGroupFieldDto::from).toList(),
                g.getCreatedAt(),
                g.getUpdatedAt()
        );
    }
}
