package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<Brand> findBySlug(String slug);

    Page<Brand> findAllByIsActive(boolean isActive, Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Brand> search(String search, Pageable pageable);
}
