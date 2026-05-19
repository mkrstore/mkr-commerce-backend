package com.mkr.commerce.catalog.dto.product;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SaveAttributesRequest(

        @NotNull
        List<AttributeEntry> attributes

) {
    public record AttributeEntry(
            @NotNull UUID   definitionId,
                     String value
    ) {}
}
