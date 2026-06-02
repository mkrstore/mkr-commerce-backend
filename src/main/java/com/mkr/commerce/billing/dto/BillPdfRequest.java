package com.mkr.commerce.billing.dto;

public record BillPdfRequest(
    String shopName,
    String shopTagline,
    String shopAddress,
    String shopPhone,
    String shopPhone2,
    String shopEmail,
    String shopGstin
) {}
