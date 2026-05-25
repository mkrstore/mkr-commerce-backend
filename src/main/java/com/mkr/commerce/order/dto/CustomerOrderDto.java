package com.mkr.commerce.order.dto;

import com.mkr.commerce.order.entity.Order;
import com.mkr.commerce.order.enums.OrderStatus;
import com.mkr.commerce.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerOrderDto(
        UUID          id,
        String        orderId,
        OrderStatus   status,
        String        paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal    totalAmount,
        String        itemsSummary,
        Instant       createdAt
) {
    public static CustomerOrderDto from(Order o) {
        return new CustomerOrderDto(
                o.getId(),
                o.getOrderId(),
                o.getStatus(),
                o.getPaymentMethod(),
                o.getPaymentStatus(),
                o.getTotalAmount(),
                o.getItemsSummary(),
                o.getCreatedAt()
        );
    }
}
