package com.mkr.commerce.customer.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.customer.enums.AuthMethod;
import com.mkr.commerce.customer.enums.CustomerType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * End-customer who registered an account on the store.
 *
 * authMethod tracks HOW the customer registered:
 *   GMAIL   — Google OAuth
 *   MOBILE  — phone + OTP
 *   USER_ID — email + password
 *
 * customerNumber is drawn from customer_num_seq (created by DataInitializer)
 * and formatted as CUS-001, CUS-002, … for display.
 *
 * totalOrders / totalSpent / pendingAmount are denormalised stats
 * updated by the order service on every order lifecycle event.
 */
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customers_email",    columnList = "email",           unique = true),
        @Index(name = "idx_customers_phone",    columnList = "phone",           unique = true),
        @Index(name = "idx_customers_google_id",columnList = "google_id"),
        @Index(name = "idx_customers_num",      columnList = "customer_number", unique = true),
        @Index(name = "idx_customers_type",     columnList = "type")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseEntity {

    // ── Identity ──────────────────────────────────────────────────────────────

    @Column(name = "customer_number", unique = true, updatable = false)
    private Long customerNumber;

    // ── Name ──────────────────────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 150)
    private String name;

    // ── Auth ──────────────────────────────────────────────────────────────────

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "google_id", length = 100)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false, length = 20)
    private AuthMethod authMethod;

    // ── Profile ───────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CustomerType type = CustomerType.RETAIL;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;

    // ── Address (all optional) ────────────────────────────────────────────────

    @Column(name = "addr_street",      length = 150)
    private String addressStreet;

    @Column(name = "addr_city",        length = 80)
    private String addressCity;

    @Column(name = "addr_state",       length = 80)
    private String addressState;

    @Column(name = "addr_postal_code", length = 20)
    private String addressPostalCode;

    @Column(name = "addr_country",     length = 80)
    private String addressCountry;

    // ── Order stats (maintained by order service) ─────────────────────────────

    @Column(name = "total_orders", nullable = false)
    @Builder.Default
    private int totalOrders = 0;

    @Column(name = "total_spent", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "pending_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal pendingAmount = BigDecimal.ZERO;

    @Column(name = "last_order_at")
    private Instant lastOrderAt;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getCustomerId() {
        return customerNumber != null ? String.format("CUS-%03d", customerNumber) : null;
    }

    public void composeName() {
        this.name = (firstName.trim() + " " + lastName.trim()).trim();
    }
}
