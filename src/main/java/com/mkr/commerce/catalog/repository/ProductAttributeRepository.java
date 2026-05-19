package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {

    @Modifying
    @Query("DELETE FROM ProductAttribute a WHERE a.product.id = :productId")
    void deleteAllByProductId(UUID productId);
}
