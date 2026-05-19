package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_variant", indexes = {
        @Index(name = "idx_product_variant_sku",        columnList = "sku",        unique = true),
        @Index(name = "idx_product_variant_product_id", columnList = "product_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "color_name", length = 80)
    private String colorName;

    @Column(name = "color_hex", length = 10)
    private String colorHex;

    @Column(length = 40)
    private String size;

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private int stockQty = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
