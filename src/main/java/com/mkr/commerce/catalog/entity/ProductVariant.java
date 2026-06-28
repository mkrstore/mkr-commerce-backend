package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(name = "price_override", precision = 12, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "price_wholesale", precision = 12, scale = 2)
    private BigDecimal priceWholesale;

    @Column(name = "price_broker", precision = 12, scale = 2)
    private BigDecimal priceBroker;

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private int stockQty = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public String label() {
        if (attributes == null || attributes.isEmpty()) return sku;
        return String.join(" / ", attributes.values());
    }
}
