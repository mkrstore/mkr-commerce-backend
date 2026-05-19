package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findAllByProductIdOrderByColorNameAscSizeAsc(UUID productId);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
}
