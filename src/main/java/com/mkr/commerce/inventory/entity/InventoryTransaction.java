package com.mkr.commerce.inventory.entity;

import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.entity.ProductVariant;
import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.inventory.enums.TransactionType;
import com.mkr.commerce.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_inv_tx_product_id", columnList = "product_id"),
        @Index(name = "idx_inv_tx_created_at", columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor
public class InventoryTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false)
    private int qty;

    @Column(name = "purchase_price_per_unit", precision = 12, scale = 2)
    private BigDecimal purchasePricePerUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(name = "vendor_name", length = 150)
    private String vendorName;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_by_email", length = 200)
    private String createdByEmail;
}
