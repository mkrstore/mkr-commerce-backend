package com.mkr.commerce.catalog.dto.lookup;

import com.mkr.commerce.catalog.entity.LookupList;

import java.util.List;
import java.util.UUID;

public record LookupListDto(
        UUID         id,
        String       name,
        String       description,
        List<String> values
) {
    public static LookupListDto from(LookupList l) {
        return new LookupListDto(
                l.getId(),
                l.getName(),
                l.getDescription(),
                l.getValues().stream().map(v -> v.getValue()).toList()
        );
    }
}
