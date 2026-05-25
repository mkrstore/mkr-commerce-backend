package com.mkr.commerce.khata.repository;

import com.mkr.commerce.khata.entity.KhataEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KhataRepository extends JpaRepository<KhataEntry, UUID> {

    List<KhataEntry> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);
}
