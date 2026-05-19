package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Product> findBySlug(String slug);

    @Query("""
            SELECT p FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:brandId    IS NULL OR p.brand.id    = :brandId)
              AND (:status     IS NULL OR p.status      = :status)
              AND (:search     IS NULL OR
                   LOWER(p.name) LIKE :search OR
                   LOWER(p.sku)  LIKE :search)
            """)
    Page<Product> findFiltered(
            @Param("categoryId") UUID categoryId,
            @Param("brandId")    UUID brandId,
            @Param("status")     ProductStatus status,
            @Param("search")     String search,
            Pageable pageable
    );

    boolean existsByCategoryId(UUID categoryId);
    boolean existsByBrandId(UUID brandId);
}
