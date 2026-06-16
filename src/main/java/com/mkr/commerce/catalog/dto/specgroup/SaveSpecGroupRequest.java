package com.mkr.commerce.catalog.dto.specgroup;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SaveSpecGroupRequest(
        @NotBlank @Size(max = 100) String     name,
        @Size(max = 500)           String     description,
        @NotNull                   List<UUID> fieldIds
) {}
