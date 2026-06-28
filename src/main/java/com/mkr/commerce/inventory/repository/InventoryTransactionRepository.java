package com.mkr.commerce.inventory.repository;

import com.mkr.commerce.inventory.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    Page<InventoryTransaction> findByVendorId(UUID vendorId, Pageable pageable);

    void deleteByProductId(UUID productId);
}
