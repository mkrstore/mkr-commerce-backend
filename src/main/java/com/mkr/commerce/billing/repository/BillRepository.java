package com.mkr.commerce.billing.repository;

import com.mkr.commerce.billing.entity.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    @Query(value = "SELECT nextval('bill_num_seq')", nativeQuery = true)
    Long nextBillNumber();

    // JOIN FETCH to avoid LazyInitializationException when building receipt response
    @Query("SELECT b FROM Bill b LEFT JOIN FETCH b.lineItems WHERE b.id = :id")
    Optional<Bill> findByIdWithItems(UUID id);

    // Step-1 of 2-step pagination: IDs only (no JOIN FETCH — safe with Pageable)
    @Query(value = "SELECT b.id FROM Bill b " +
                   "WHERE (:search IS NULL OR LOWER(b.customerName) LIKE :search OR b.customerPhone LIKE :search) " +
                   "ORDER BY b.createdAt DESC",
           countQuery = "SELECT COUNT(b) FROM Bill b " +
                        "WHERE (:search IS NULL OR LOWER(b.customerName) LIKE :search OR b.customerPhone LIKE :search)")
    Page<UUID> findIdsForList(@Param("search") String search, Pageable pageable);

    // Step-2: full bills with items for the given IDs
    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.lineItems WHERE b.id IN :ids")
    List<Bill> findByIdsWithItems(@Param("ids") List<UUID> ids);

    // All bills for a customer, with items, newest first
    @Query("SELECT DISTINCT b FROM Bill b LEFT JOIN FETCH b.lineItems " +
           "WHERE b.customer.id = :customerId ORDER BY b.createdAt DESC")
    List<Bill> findByCustomerIdWithItemsOrderByCreatedAtDesc(@Param("customerId") UUID customerId);
}
