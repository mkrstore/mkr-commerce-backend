package com.mkr.commerce.customer.dto;

import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.customer.enums.AuthMethod;
import com.mkr.commerce.customer.enums.CustomerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerDetailDto(
        UUID         id,
        String       customerId,
        String       firstName,
        String       lastName,
        String       name,
        String       email,
        String       phone,
        AuthMethod   authMethod,
        CustomerType type,
        boolean      isActive,
        Instant      createdAt,
        Instant      updatedAt,
        Instant      lastOrderAt,
        int          totalOrders,
        BigDecimal   totalSpent,
        BigDecimal   pendingAmount,
        String       addressStreet,
        String       addressCity,
        String       addressMandal,
        String       addressDistrict,
        String       addressState,
        String       addressPostalCode
) {
    public static CustomerDetailDto from(Customer c) {
        return new CustomerDetailDto(
                c.getId(),
                c.getCustomerId(),
                c.getFirstName(),
                c.getLastName(),
                c.getName(),
                c.getEmail(),
                c.getPhone(),
                c.getAuthMethod(),
                c.getType(),
                c.isActive(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getLastOrderAt(),
                c.getTotalOrders(),
                c.getTotalSpent(),
                c.getPendingAmount(),
                c.getAddressStreet(),
                c.getAddressCity(),
                c.getAddressMandal(),
                c.getAddressDistrict(),
                c.getAddressState(),
                c.getAddressPostalCode()
        );
    }
}
