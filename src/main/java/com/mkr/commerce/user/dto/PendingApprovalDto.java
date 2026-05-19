package com.mkr.commerce.user.dto;

import com.mkr.commerce.user.entity.PendingApproval;

import java.time.Instant;
import java.util.UUID;

public record PendingApprovalDto(
        UUID    id,
        String  approvalType,
        String  status,
        String  requesterName,
        String  requesterRole,
        UUID    targetUserId,
        String  targetUserName,
        String  targetUserEmail,
        String  payload,          // raw JSON — frontend deserialises to show change summary
        String  reviewedByName,
        String  reviewComment,
        Instant createdAt,
        Instant reviewedAt
) {
    public static PendingApprovalDto from(PendingApproval p) {
        return new PendingApprovalDto(
                p.getId(),
                p.getApprovalType().name(),
                p.getStatus().name(),
                p.getRequester().getName(),
                p.getRequester().getRole().name(),
                p.getTargetUser().getId(),
                p.getTargetUser().getName(),
                p.getTargetUser().getEmail(),
                p.getPayload(),
                p.getReviewedBy() != null ? p.getReviewedBy().getName() : null,
                p.getReviewComment(),
                p.getCreatedAt(),
                p.getReviewedAt()
        );
    }
}
