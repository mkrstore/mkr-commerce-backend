package com.mkr.commerce.user.repository;

import com.mkr.commerce.user.entity.InvitationToken;
import com.mkr.commerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByToken(String token);

    /** Find the most-recent unused, non-expired invitation for a user. */
    @Query("SELECT t FROM InvitationToken t WHERE t.user = :user AND t.used = false AND t.expiresAt > :now ORDER BY t.expiresAt DESC")
    Optional<InvitationToken> findActiveByUser(@Param("user") User user, @Param("now") Instant now);

    /** Invalidate any previous pending invitation before issuing a new one. */
    @Modifying
    @Query("UPDATE InvitationToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllForUser(User user);
}
