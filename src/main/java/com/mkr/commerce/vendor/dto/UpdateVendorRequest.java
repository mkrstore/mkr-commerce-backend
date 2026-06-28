package com.mkr.commerce.vendor.dto;

import jakarta.validation.constraints.Size;

public record UpdateVendorRequest(
        @Size(max = 150) String  name,
        @Size(max = 20)  String  phone,
        @Size(max = 200) String  email,
                         String  address,
        @Size(max = 20)  String  gstin,
        @Size(max = 500) String  notes,
                         Boolean isActive
) {}
