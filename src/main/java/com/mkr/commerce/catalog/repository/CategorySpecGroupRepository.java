package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.CategorySpecGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategorySpecGroupRepository extends JpaRepository<CategorySpecGroup, UUID> {
    boolean existsByCategory_IdAndGroup_Id(UUID categoryId, UUID groupId);
    Optional<CategorySpecGroup> findByCategory_IdAndGroup_Id(UUID categoryId, UUID groupId);
    List<CategorySpecGroup> findByCategory_IdOrderBySortOrderAsc(UUID categoryId);
    int countByCategory_Id(UUID categoryId);
}
