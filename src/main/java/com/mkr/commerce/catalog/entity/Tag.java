package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag", indexes = {
        @Index(name = "idx_tag_name", columnList = "name", unique = true),
        @Index(name = "idx_tag_slug", columnList = "slug", unique = true)
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Tag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;
}
