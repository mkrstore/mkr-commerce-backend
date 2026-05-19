package com.mkr.commerce.catalog.dto.category;

import com.mkr.commerce.catalog.enums.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAttributeDefinitionRequest(

        @NotBlank @Size(max = 100)
        String label,

        @NotNull
        FieldType fieldType,

        String  options,
        @Size(max = 30)  String unit,
        @Size(max = 500) String defaultValue,
        boolean required,
        int     sortOrder
) {}
