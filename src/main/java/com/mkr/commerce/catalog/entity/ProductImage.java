package com.mkr.commerce.catalog.entity;

import com.mkr.commerce.catalog.enums.MediaType;
import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_image", indexes = {
        @Index(name = "idx_product_image_product_id", columnList = "product_id"),
        @Index(name = "idx_product_image_type",       columnList = "media_type")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 20)
    @Builder.Default
    private MediaType mediaType = MediaType.IMAGE_GALLERY;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "public_id", length = 200)
    private String publicId;

    @Column(name = "alt_text", length = 200)
    private String altText;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    public boolean isPrimary() {
        return mediaType == MediaType.IMAGE_PRIMARY;
    }
}
