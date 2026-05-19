package com.mkr.commerce.user.repository;

import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    /** Draws the next value from the PostgreSQL sequence — thread-safe. */
    @Query(value = "SELECT nextval('employee_id_seq')", nativeQuery = true)
    Long nextEmployeeId();

    /**
     * Paginated staff list with optional filters.
     *
     * @param roles        Set of roles the caller is permitted to see
     * @param active       null = both, true = active only, false = inactive only
     * @param search       nullable — matched against name or email (case-insensitive)
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.role IN :roles
          AND (:active IS NULL OR u.isActive = :active)
          AND (:search IS NULL OR LOWER(u.name) LIKE :search OR LOWER(u.email) LIKE :search)
        ORDER BY u.createdAt DESC
        """)
    Page<User> findStaff(
        @Param("roles")  Set<UserRole> roles,
        @Param("active") Boolean active,
        @Param("search") String search,
        Pageable pageable
    );
}
