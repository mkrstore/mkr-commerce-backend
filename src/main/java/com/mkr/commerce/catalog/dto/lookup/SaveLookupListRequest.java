package com.mkr.commerce.catalog.dto.lookup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SaveLookupListRequest(

        @NotBlank @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @NotNull
        List<@NotBlank @Size(max = 200) String> values
) {}
