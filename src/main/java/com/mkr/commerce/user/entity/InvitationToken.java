package com.mkr.commerce.user.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One-time token sent to a newly created staff member.
 *
 * Flow:
 *   1. Admin creates a staff account (no password yet, isActive = false).
 *   2. This token is generated and the invitation email is sent.
 *   3. Staff clicks the link → POST /api/auth/set-password → password set, account activated.
 *   4. Token is marked used. Expires in 48 hours if not clicked.
 */
@Entity
@Table(
    name = "invitation_tokens",
    indexes = {
        @Index(name = "idx_invitation_token", columnList = "token", unique = true)
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    /** Invitation link no longer valid. */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
