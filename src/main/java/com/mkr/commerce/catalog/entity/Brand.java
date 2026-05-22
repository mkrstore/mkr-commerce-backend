package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "brand", indexes = {
        @Index(name = "idx_brand_name", columnList = "name", unique = true),
        @Index(name = "idx_brand_slug", columnList = "slug", unique = true)
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Brand extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "logo_public_id", length = 200)
    private String logoPublicId;

    @Column(name = "logo_is_video", columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean logoIsVideo = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
