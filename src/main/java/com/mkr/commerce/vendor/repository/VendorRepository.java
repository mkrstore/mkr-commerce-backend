package com.mkr.commerce.vendor.repository;

import com.mkr.commerce.vendor.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findAllByOrderByNameAsc();

    List<Vendor> findByIsActiveTrueOrderByNameAsc();

    @Query("""
            SELECT v FROM Vendor v
            WHERE (:search IS NULL OR LOWER(v.name) LIKE :search
                   OR LOWER(v.phone) LIKE :search
                   OR LOWER(v.gstin) LIKE :search)
            ORDER BY v.name ASC
            """)
    Page<Vendor> findFiltered(@Param("search") String search, Pageable pageable);
}
