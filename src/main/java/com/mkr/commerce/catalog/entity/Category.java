package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLOrder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_slug",      columnList = "slug",      unique = true),
        @Index(name = "idx_category_parent_id", columnList = "parent_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Category extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "image_public_id", length = 200)
    private String imagePublicId;

    @Column(name = "image_is_video", columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean imageIsVideo = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @SQLOrder("sort_order ASC")
    @Builder.Default
    private List<AttributeDefinition> attributeDefinitions = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
