package com.mkr.commerce.user.repository;

import com.mkr.commerce.user.entity.InvitationToken;
import com.mkr.commerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByToken(String token);

    /** Invalidate any previous pending invitation before issuing a new one. */
    @Modifying
    @Query("UPDATE InvitationToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllForUser(User user);
}
