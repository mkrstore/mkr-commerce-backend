package com.mkr.commerce.billing.entity;

import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.common.converter.StringListConverter;
import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bill_line_items", indexes = {
    @Index(name = "idx_bli_bill", columnList = "bill_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BillLineItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bill_id", nullable = false, updatable = false)
    private Bill bill;

    // Nullable — product may be deactivated/deleted later; name/sku are preserved
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 80)
    private String productSku;

    @Column(nullable = false)
    private int qty;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = BigDecimal.ZERO;

    // lineTotal = (unitPrice * qty) + gstAmount
    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "gst_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    // One serial number per unit sold (e.g. IMEI numbers). Stored as JSON array.
    @Convert(converter = StringListConverter.class)
    @Column(name = "serial_numbers", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> serialNumbers = new ArrayList<>();
}
