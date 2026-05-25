package com.mkr.commerce.customer.repository;

import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.customer.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByGoogleId(String googleId);
    Optional<Customer> findByCustomerNumber(Long customerNumber);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    /** Draws the next value from the PostgreSQL sequence. */
    @Query(value = "SELECT nextval('customer_num_seq')", nativeQuery = true)
    Long nextCustomerNumber();

    /**
     * Paginated customer list with optional filters.
     *
     * @param type          null = all types
     * @param pendingFilter null = include all; non-null = only customers with pendingAmount > 0
     * @param search        null = no text filter; otherwise matched against name/email/phone
     * @param customerNum   null = no ID filter; otherwise exact match on customerNumber
     */
    @Query(
        value = """
            SELECT c FROM Customer c
            WHERE (:type IS NULL OR c.type = :type)
              AND (:pendingFilter IS NULL OR c.pendingAmount > 0)
              AND (
                :search IS NULL
                OR LOWER(c.name)  LIKE :search
                OR LOWER(c.email) LIKE :search
                OR c.phone        LIKE :search
              )
              AND (:customerNum IS NULL OR c.customerNumber = :customerNum)
            """,
        countQuery = """
            SELECT COUNT(c) FROM Customer c
            WHERE (:type IS NULL OR c.type = :type)
              AND (:pendingFilter IS NULL OR c.pendingAmount > 0)
              AND (
                :search IS NULL
                OR LOWER(c.name)  LIKE :search
                OR LOWER(c.email) LIKE :search
                OR c.phone        LIKE :search
              )
              AND (:customerNum IS NULL OR c.customerNumber = :customerNum)
            """
    )
    Page<Customer> findCustomers(
        @Param("type")        CustomerType type,
        @Param("pendingFilter") Boolean pendingFilter,
        @Param("search")      String search,
        @Param("customerNum") Long customerNum,
        Pageable pageable
    );
}
