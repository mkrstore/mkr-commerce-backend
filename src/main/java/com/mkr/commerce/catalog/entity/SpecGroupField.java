package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "spec_group_field",
    uniqueConstraints = @UniqueConstraint(name = "uk_spec_group_field", columnNames = {"group_id", "field_id"})
)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SpecGroupField extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private SpecGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_id", nullable = false)
    private LookupList field;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
