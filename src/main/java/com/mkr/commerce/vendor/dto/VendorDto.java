package com.mkr.commerce.vendor.dto;

import com.mkr.commerce.vendor.entity.Vendor;

import java.time.Instant;
import java.util.UUID;

public record VendorDto(
        UUID    id,
        String  name,
        String  phone,
        String  email,
        String  address,
        String  gstin,
        String  notes,
        boolean isActive,
        Instant createdAt
) {
    public static VendorDto from(Vendor v) {
        return new VendorDto(
                v.getId(), v.getName(), v.getPhone(), v.getEmail(),
                v.getAddress(), v.getGstin(), v.getNotes(),
                v.isActive(), v.getCreatedAt()
        );
    }
}
