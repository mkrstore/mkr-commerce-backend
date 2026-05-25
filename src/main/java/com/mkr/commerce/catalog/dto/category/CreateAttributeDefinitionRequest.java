package com.mkr.commerce.catalog.dto.category;

import com.mkr.commerce.catalog.enums.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAttributeDefinitionRequest(

        @NotBlank @Size(max = 100)
        String label,

        @NotBlank @Size(max = 100)
        String fieldKey,

        @NotNull
        FieldType fieldType,

        String  options,       // JSON array string — required for SELECT / MULTISELECT
        @Size(max = 30)  String unit,
        @Size(max = 500) String defaultValue,
        boolean required,
        int     sortOrder,

        @Size(max = 100)
        String groupName       // e.g. "Battery", "Processor" — null = General
) {}
