package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, UUID> {

    List<AttributeDefinition> findAllByCategoryIdOrderBySortOrderAsc(UUID categoryId);

    Optional<AttributeDefinition> findByIdAndCategoryId(UUID id, UUID categoryId);

    boolean existsByFieldKeyAndCategoryId(String fieldKey, UUID categoryId);

    boolean existsByFieldKeyAndCategoryIdAndIdNot(String fieldKey, UUID categoryId, UUID id);

    @Modifying
    @Query("DELETE FROM AttributeDefinition a WHERE a.category.id = :categoryId")
    void deleteAllByCategoryId(UUID categoryId);
}
