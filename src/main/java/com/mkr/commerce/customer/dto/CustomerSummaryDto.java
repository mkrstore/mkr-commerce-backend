package com.mkr.commerce.customer.dto;

import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.customer.enums.AuthMethod;
import com.mkr.commerce.customer.enums.CustomerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerSummaryDto(
        UUID         id,
        String       customerId,
        String       name,
        String       email,
        String       phone,
        AuthMethod   authMethod,
        CustomerType type,
        boolean      isActive,
        Instant      createdAt,
        Instant      lastOrderAt,
        int          totalOrders,
        BigDecimal   totalSpent,
        BigDecimal   pendingAmount
) {
    public static CustomerSummaryDto from(Customer c) {
        return new CustomerSummaryDto(
                c.getId(),
                c.getCustomerId(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getAuthMethod(),
                c.getType(),
                c.isActive(),
                c.getCreatedAt(),
                c.getLastOrderAt(),
                c.getTotalOrders(),
                c.getTotalSpent(),
                c.getPendingAmount()
        );
    }
}
