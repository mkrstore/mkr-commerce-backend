package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.catalog.enums.ProductStatus;
import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Entity
@Table(name = "product", indexes = {
        @Index(name = "idx_product_slug",        columnList = "slug",        unique = true),
        @Index(name = "idx_product_sku",         columnList = "sku",         unique = true),
        @Index(name = "idx_product_category_id", columnList = "category_id"),
        @Index(name = "idx_product_brand_id",    columnList = "brand_id"),
        @Index(name = "idx_product_status",      columnList = "status")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_vendor_id")
    private Vendor preferredVendor;

    // ── Pricing (three tiers) ─────────────────────────────────────────────────

    @Column(name = "price_retail", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceRetail;

    @Column(name = "price_wholesale", precision = 12, scale = 2)
    private BigDecimal priceWholesale;

    @Column(name = "price_broker", precision = 12, scale = 2)
    private BigDecimal priceBroker;

    @Column(name = "min_qty_wholesale", nullable = false)
    @Builder.Default
    private int minQtyWholesale = 10;

    // ── Tax ───────────────────────────────────────────────────────────────────

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = BigDecimal.valueOf(18);

    @Column(name = "gst_included", nullable = false)
    @Builder.Default
    private boolean gstIncluded = false;

    // ── Stock ─────────────────────────────────────────────────────────────────

    @Column(name = "stock_qty", nullable = false)
    @Builder.Default
    private int stockQty = 0;

    // ── Physical ──────────────────────────────────────────────────────────────

    @Column(precision = 10, scale = 2)
    private BigDecimal weight;              // grams

    @Column(length = 50)
    private String dimensions;             // e.g. "10x5x3 cm"

    // ── Status ────────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    // ── Relations ─────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @SQLOrder("sort_order ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    @org.hibernate.annotations.BatchSize(size = 50)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductAttribute> attributes = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "product_tag_link",
            joinColumns        = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<Tag> tags = new HashSet<>();
}
