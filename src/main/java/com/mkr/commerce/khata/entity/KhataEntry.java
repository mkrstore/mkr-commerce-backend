package com.mkr.commerce.khata.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.khata.enums.KhataEntryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One line in a customer's khata (ledger) book.
 *
 * balance = running pendingAmount of the customer AFTER this entry.
 * debit   = amount added to the outstanding balance (customer owes more).
 * credit  = amount subtracted from the outstanding balance (customer paid).
 *
 * Exactly one of debit/credit is non-zero per entry.
 * The Customer.pendingAmount field is kept in sync on every write.
 */
@Entity
@Table(
    name = "khata_entries",
    indexes = {
        @Index(name = "idx_khata_customer", columnList = "customer_id"),
        @Index(name = "idx_khata_created",  columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhataEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private KhataEntryType entryType;

    @Column(nullable = false, length = 250)
    private String description;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal credit = BigDecimal.ZERO;

    /** Running pendingAmount after this entry. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    /** Optional — links back to an order (populated by order service). */
    @Column(name = "order_id", length = 50)
    private String orderId;

    /** Payment method for MANUAL_CREDIT entries (Cash, UPI, etc.). */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(length = 300)
    private String notes;
}
