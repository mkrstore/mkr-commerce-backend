package com.mkr.commerce.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ErrorCode;
import com.mkr.commerce.common.exception.ForbiddenException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.user.dto.PendingApprovalDto;
import com.mkr.commerce.user.dto.UpdateStaffRequest;
import com.mkr.commerce.user.entity.PendingApproval;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.ApprovalStatus;
import com.mkr.commerce.user.enums.ApprovalType;
import com.mkr.commerce.user.enums.AuditAction;
import com.mkr.commerce.user.repository.PendingApprovalRepository;
import com.mkr.commerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PendingApprovalService {

    private final PendingApprovalRepository approvalRepository;
    private final UserRepository            userRepository;
    private final AuditLogService           auditLogService;
    private final ObjectMapper              objectMapper;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public PendingApproval createEditProfileApproval(User requester, User target, UpdateStaffRequest request) {
        // Supersede any existing pending approval of the same type for this target
        approvalRepository.supersedePending(target, ApprovalType.EDIT_PROFILE);

        String payload;
        try {
            payload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialise approval payload", e);
        }

        PendingApproval approval = PendingApproval.builder()
                .requester(requester)
                .targetUser(target)
                .approvalType(ApprovalType.EDIT_PROFILE)
                .payload(payload)
                .status(ApprovalStatus.PENDING)
                .build();

        approvalRepository.save(approval);
        auditLogService.log(target.getId(), requester, AuditAction.PROFILE_UPDATED,
                "Edit requested — awaiting Super Admin approval");
        log.info("Pending approval created by {} for {}", requester.getEmail(), target.getEmail());
        return approval;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PendingApprovalDto> listPending() {
        return approvalRepository.findByStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING)
                .stream().map(PendingApprovalDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingApprovalDto> listForUser(UUID targetUserId) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return approvalRepository.findByTargetUserAndStatusOrderByCreatedAtDesc(target, ApprovalStatus.PENDING)
                .stream().map(PendingApprovalDto::from).toList();
    }

    // ── Approve ───────────────────────────────────────────────────────────────

    @Transactional
    public void approve(UUID approvalId, User reviewer, String comment) {
        PendingApproval approval = findPending(approvalId);

        if (approval.getApprovalType() == ApprovalType.EDIT_PROFILE) {
            User target = approval.getTargetUser();
            UpdateStaffRequest request = deserialise(approval.getPayload());
            applyFields(target, request);
            userRepository.save(target);
            auditLogService.log(target.getId(), reviewer, AuditAction.PROFILE_UPDATED,
                    "Approved by " + reviewer.getName()
                    + (comment != null && !comment.isBlank() ? " — " + comment : ""));
        }

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setReviewedBy(reviewer);
        approval.setReviewComment(comment);
        approval.setReviewedAt(Instant.now());
        approvalRepository.save(approval);

        log.info("Approval {} approved by {}", approvalId, reviewer.getEmail());
    }

    // ── Reject ────────────────────────────────────────────────────────────────

    @Transactional
    public void reject(UUID approvalId, User reviewer, String comment) {
        PendingApproval approval = findPending(approvalId);

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setReviewedBy(reviewer);
        approval.setReviewComment(comment);
        approval.setReviewedAt(Instant.now());
        approvalRepository.save(approval);

        User target = approval.getTargetUser();
        auditLogService.log(target.getId(), reviewer, AuditAction.PROFILE_UPDATED,
                "Edit request rejected by " + reviewer.getName()
                + (comment != null && !comment.isBlank() ? " — " + comment : ""));

        log.info("Approval {} rejected by {}", approvalId, reviewer.getEmail());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PendingApproval findPending(UUID id) {
        PendingApproval approval = approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found"));
        if (!approval.isPending()) {
            throw new BadRequestException("This request has already been " + approval.getStatus().name().toLowerCase() + ".",
                    ErrorCode.FORBIDDEN);
        }
        return approval;
    }

    private UpdateStaffRequest deserialise(String payload) {
        try {
            return objectMapper.readValue(payload, UpdateStaffRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialise approval payload", e);
        }
    }

    /** Apply non-null fields from request to user entity. Empty string = clear optional field. */
    static void applyFields(User staff, UpdateStaffRequest req) {
        boolean nameChanged = false;

        if (req.firstName() != null && !req.firstName().isBlank()) {
            staff.setFirstName(req.firstName().trim()); nameChanged = true;
        }
        if (req.middleName() != null) {
            staff.setMiddleName(req.middleName().isBlank() ? null : req.middleName().trim()); nameChanged = true;
        }
        if (req.lastName() != null && !req.lastName().isBlank()) {
            staff.setLastName(req.lastName().trim()); nameChanged = true;
        }
        if (nameChanged) staff.composeName();

        if (req.email()            != null && !req.email().isBlank())        staff.setEmail(req.email().toLowerCase().trim());
        if (req.department()       != null && !req.department().isBlank())   staff.setDepartment(req.department().trim());
        if (req.mobileNumber()     != null && !req.mobileNumber().isBlank()) staff.setMobileNumber(req.mobileNumber().trim());
        if (req.alternativePhone() != null) staff.setAlternativePhone(req.alternativePhone().isBlank() ? null : req.alternativePhone().trim());
        if (req.addressBuilding()  != null) staff.setAddressBuilding(req.addressBuilding().isBlank()  ? null : req.addressBuilding().trim());
        if (req.addressStreet()    != null) staff.setAddressStreet(req.addressStreet().isBlank()      ? null : req.addressStreet().trim());
        if (req.addressCity()      != null) staff.setAddressCity(req.addressCity().isBlank()          ? null : req.addressCity().trim());
        if (req.addressState()     != null) staff.setAddressState(req.addressState().isBlank()        ? null : req.addressState().trim());
        if (req.addressPostalCode()!= null) staff.setAddressPostalCode(req.addressPostalCode().isBlank() ? null : req.addressPostalCode().trim());
        if (req.addressCountry()   != null) staff.setAddressCountry(req.addressCountry().isBlank()    ? null : req.addressCountry().trim());
    }
}
