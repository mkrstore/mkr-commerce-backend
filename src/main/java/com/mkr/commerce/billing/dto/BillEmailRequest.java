package com.mkr.commerce.billing.dto;

public record BillEmailRequest(
    String shopName,
    String shopTagline,
    String shopAddress,
    String shopPhone,
    String shopPhone2,
    String shopEmail,
    String shopGstin,
    String toEmail   // optional override — null uses customer's registered email
) {}
