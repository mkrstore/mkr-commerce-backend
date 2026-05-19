package com.mkr.commerce.auth.entity;

import com.mkr.commerce.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores the last N hashed passwords for a user.
 *
 * When resetting, we compare the new password against all
 * stored hashes. If it matches any, the reset is rejected.
 *
 * N is controlled by app.password-reset.history-count (default 2).
 * Oldest entries beyond N are pruned after each successful reset.
 */
@Entity
@Table(
    name = "password_history",
    indexes = {
        @Index(name = "idx_ph_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
