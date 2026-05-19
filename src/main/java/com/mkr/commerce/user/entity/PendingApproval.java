package com.mkr.commerce.user.entity;

import com.mkr.commerce.common.entity.BaseEntity;
import com.mkr.commerce.user.enums.ApprovalStatus;
import com.mkr.commerce.user.enums.ApprovalType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Stores change requests that require a second approver (4-eye principle).
 *
 * payload — JSON string of the requested change (e.g. serialised UpdateStaffRequest).
 * When status → APPROVED the service reads the payload, applies the change, and writes an audit log.
 */
@Entity
@Table(
    name = "pending_approvals",
    indexes = {
        @Index(name = "idx_pa_target",  columnList = "target_user_id"),
        @Index(name = "idx_pa_status",  columnList = "status"),
        @Index(name = "idx_pa_created", columnList = "created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingApproval extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalType approvalType;

    /** JSON-serialised payload of the change being requested. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(length = 500)
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }
}
