package com.mkr.commerce.billing.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bills", indexes = {
    @Index(name = "idx_bills_customer", columnList = "customer_id"),
    @Index(name = "idx_bills_number",   columnList = "bill_number", unique = true),
    @Index(name = "idx_bills_created",  columnList = "created_at")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Bill extends BaseEntity {

    @Column(name = "bill_number", unique = true, updatable = false)
    private Long billNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Denormalized for display and historical accuracy
    @Column(name = "customer_phone", length = 20, nullable = false)
    private String customerPhone;

    @Column(name = "customer_name", length = 150, nullable = false)
    private String customerName;

    @Column(name = "gst_enabled", nullable = false)
    @Builder.Default
    private boolean gstEnabled = false;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "total_discount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDiscount = BigDecimal.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal grandTotal;

    @Column(name = "paid_now", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidNow;

    @Column(name = "khata_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal khataAmount = BigDecimal.ZERO;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BillLineItem> lineItems = new ArrayList<>();

    public String getBillId() {
        return billNumber != null ? String.format("MKR-BILL-%04d", billNumber) : null;
    }
}
