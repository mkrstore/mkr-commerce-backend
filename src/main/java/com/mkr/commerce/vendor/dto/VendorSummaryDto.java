package com.mkr.commerce.vendor.dto;

import com.mkr.commerce.vendor.entity.Vendor;

import java.util.UUID;

public record VendorSummaryDto(UUID id, String name) {
    public static VendorSummaryDto from(Vendor v) {
        return new VendorSummaryDto(v.getId(), v.getName());
    }
}
