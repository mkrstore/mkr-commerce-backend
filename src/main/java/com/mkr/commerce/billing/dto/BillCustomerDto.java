package com.mkr.commerce.billing.dto;

import com.mkr.commerce.customer.entity.Customer;

import java.math.BigDecimal;
import java.util.UUID;

public record BillCustomerDto(
        UUID       id,
        String     customerId,
        String     name,
        String     phone,
        String     email,
        BigDecimal pendingAmount  // balance AFTER this bill
) {
    public static BillCustomerDto from(Customer c) {
        return new BillCustomerDto(
                c.getId(),
                c.getCustomerId(),
                c.getName(),
                c.getPhone(),
                c.getEmail(),
                c.getPendingAmount()
        );
    }
}
