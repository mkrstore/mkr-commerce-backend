package com.mkr.commerce.user.repository;

import com.mkr.commerce.user.entity.PendingApproval;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.ApprovalStatus;
import com.mkr.commerce.user.enums.ApprovalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PendingApprovalRepository extends JpaRepository<PendingApproval, UUID> {

    List<PendingApproval> findByStatusOrderByCreatedAtDesc(ApprovalStatus status);

    List<PendingApproval> findByTargetUserAndStatusOrderByCreatedAtDesc(User targetUser, ApprovalStatus status);

    /** Cancel any existing pending approval of the same type for the same target — prevents duplicates. */
    @Query("UPDATE PendingApproval p SET p.status = 'REJECTED', p.reviewComment = 'Superseded by new request' " +
           "WHERE p.targetUser = :target AND p.approvalType = :type AND p.status = 'PENDING'")
    @org.springframework.data.jpa.repository.Modifying
    void supersedePending(@Param("target") User target, @Param("type") ApprovalType type);
}
