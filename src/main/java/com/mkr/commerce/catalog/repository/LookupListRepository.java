package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.LookupList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LookupListRepository extends JpaRepository<LookupList, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    List<LookupList> findAllByOrderByNameAsc();
}
