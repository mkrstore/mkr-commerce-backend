package com.mkr.commerce.auth.repository;

import com.mkr.commerce.auth.entity.PasswordHistory;
import com.mkr.commerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    /** Ordered newest first — used for history-count enforcement. */
    List<PasswordHistory> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Deletes oldest entries beyond the allowed history count.
     * Called after every successful password save so the table stays lean.
     *
     * Uses a subquery to find IDs to keep (newest N), then deletes the rest.
     */
    @Modifying
    @Query(value = """
        DELETE FROM password_history
        WHERE user_id = :userId
          AND id NOT IN (
              SELECT id FROM password_history
              WHERE user_id = :userId
              ORDER BY created_at DESC
              LIMIT :keepCount
          )
        """, nativeQuery = true)
    void pruneOldEntries(UUID userId, int keepCount);
}
