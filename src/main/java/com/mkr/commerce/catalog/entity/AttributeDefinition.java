package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.catalog.enums.FieldType;
import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "attribute_definition", indexes = {
        @Index(name = "idx_attr_def_category_id", columnList = "category_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttributeDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Display label shown in the form and product page. */
    @Column(nullable = false, length = 100)
    private String label;

    /** Stable snake_case key used in API and customer portal. */
    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private FieldType fieldType;

    /** JSON array of option strings — used for SELECT and MULTISELECT types. */
    @Column(columnDefinition = "TEXT")
    private String options;

    /** Physical unit shown next to the value, e.g. L, inch, W, kg, ★ */
    @Column(length = 30)
    private String unit;

    /** Pre-filled value when the admin opens the create-product form. */
    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
