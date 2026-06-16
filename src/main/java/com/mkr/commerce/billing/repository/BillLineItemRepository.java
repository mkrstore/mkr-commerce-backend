package com.mkr.commerce.billing.repository;

import com.mkr.commerce.billing.entity.BillLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BillLineItemRepository extends JpaRepository<BillLineItem, UUID> {

    // Exact JSON-array membership check — "SN123" must appear as a value in the array,
    // not just as a substring.  Uses the PostgreSQL JSONB containment operator (@>).
    @Query(value =
        "SELECT b.bill_number " +
        "FROM bill_line_items l " +
        "JOIN bills b ON l.bill_id = b.id " +
        "WHERE l.serial_numbers IS NOT NULL " +
        "  AND CAST(l.serial_numbers AS jsonb) @> CAST(:snJson AS jsonb) " +
        "LIMIT 1",
        nativeQuery = true)
    Optional<Long> findBillNumberBySerialNumber(@Param("snJson") String snJson);
}
