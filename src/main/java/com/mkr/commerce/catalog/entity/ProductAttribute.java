package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attribute", indexes = {
        @Index(name = "idx_product_attr_product_id",    columnList = "product_id"),
        @Index(name = "idx_product_attr_definition_id", columnList = "definition_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductAttribute extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "definition_id", nullable = false)
    private AttributeDefinition definition;

    /** Stored as String for all field types — client interprets via definition.fieldType */
    @Column(name = "attr_value", length = 1000)
    private String value;
}
