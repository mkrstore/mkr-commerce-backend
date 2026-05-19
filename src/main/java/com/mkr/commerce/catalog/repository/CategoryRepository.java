package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);
    boolean existsByNameAndIdNot(String name, UUID id);

    Optional<Category> findBySlug(String slug);

    List<Category> findAllByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();

    List<Category> findAllByParentIdAndIsActiveTrue(UUID parentId);

    @Query("SELECT c FROM Category c WHERE c.isActive = true AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Category> searchActive(String search, Pageable pageable);

    Page<Category> findAllByIsActive(boolean isActive, Pageable pageable);

    boolean existsByParentId(UUID parentId);
}
