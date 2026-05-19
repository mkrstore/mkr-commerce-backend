package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.ProductImage;
import com.mkr.commerce.catalog.enums.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findAllByProductIdOrderBySortOrderAsc(UUID productId);

    int countByProductId(UUID productId);

    int countByProductIdAndMediaType(UUID productId, MediaType mediaType);

    @Modifying
    @Query("UPDATE ProductImage i SET i.mediaType = com.mkr.commerce.catalog.enums.MediaType.IMAGE_GALLERY " +
           "WHERE i.product.id = :productId AND i.mediaType = com.mkr.commerce.catalog.enums.MediaType.IMAGE_PRIMARY")
    void clearPrimaryForProduct(UUID productId);
}
