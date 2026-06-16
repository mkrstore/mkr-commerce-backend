package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "category_spec_group",
    uniqueConstraints = @UniqueConstraint(name = "uk_category_spec_group", columnNames = {"category_id", "group_id"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CategorySpecGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private SpecGroup group;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
