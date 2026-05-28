package com.mkr.commerce.order.dto;

import com.mkr.commerce.billing.entity.Bill;
import com.mkr.commerce.order.entity.Order;
import com.mkr.commerce.order.enums.OrderStatus;
import com.mkr.commerce.order.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static CustomerOrderDto fromBill(Bill b) {
        String summary = b.getLineItems().stream()
                .limit(3)
                .map(li -> li.getProductName() + " ×" + li.getQty())
                .collect(Collectors.joining(", "));
        if (b.getLineItems().size() > 3) summary += " …";

        PaymentStatus payStatus = b.getKhataAmount().compareTo(BigDecimal.ZERO) > 0
                ? PaymentStatus.PENDING
                : PaymentStatus.PAID;

        return new CustomerOrderDto(
                b.getId(),
                b.getBillId(),
                OrderStatus.DELIVERED,
                b.getPaymentMethod(),
                payStatus,
                b.getGrandTotal(),
                summary,
                b.getCreatedAt()
        );
    }
}
