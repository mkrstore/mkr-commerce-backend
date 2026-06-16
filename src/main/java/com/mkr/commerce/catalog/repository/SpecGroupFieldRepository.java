package com.mkr.commerce.catalog.repository;

import com.mkr.commerce.catalog.entity.SpecGroupField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpecGroupFieldRepository extends JpaRepository<SpecGroupField, UUID> {
}
