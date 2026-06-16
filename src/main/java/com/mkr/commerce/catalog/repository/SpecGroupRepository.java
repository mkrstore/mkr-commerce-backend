package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.SpecGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpecGroupRepository extends JpaRepository<SpecGroup, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    List<SpecGroup> findAllByOrderByNameAsc();
}
