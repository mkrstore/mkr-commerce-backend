package com.mkr.commerce.order.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.order.enums.OrderStatus;
import com.mkr.commerce.order.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_customer",      columnList = "customer_id"),
        @Index(name = "idx_orders_status",         columnList = "status"),
        @Index(name = "idx_orders_order_number",   columnList = "order_number", unique = true),
        @Index(name = "idx_orders_created",        columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "order_number", unique = true, updatable = false)
    private Long orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    /** Denormalised summary for list views, e.g. "Nike Polo T-Shirt ×1, Adidas Shoes ×2" */
    @Column(name = "items_summary", nullable = false, length = 500)
    private String itemsSummary;

    public String getOrderId() {
        return orderNumber != null ? String.format("MKR-%05d", orderNumber) : null;
    }
}
