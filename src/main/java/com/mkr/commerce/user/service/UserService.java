package com.mkr.commerce.user.service;

import com.mkr.commerce.auth.service.EmailService;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ErrorCode;
import com.mkr.commerce.common.exception.ForbiddenException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.user.dto.AuditLogDto;
import com.mkr.commerce.user.dto.CreateStaffRequest;
import com.mkr.commerce.user.dto.StaffDto;
import com.mkr.commerce.user.dto.UpdateRoleRequest;
import com.mkr.commerce.user.dto.UpdateStaffRequest;
import com.mkr.commerce.user.dto.UpdateStatusRequest;
import com.mkr.commerce.user.entity.InvitationToken;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.AuditAction;
import com.mkr.commerce.user.enums.UserRole;
import com.mkr.commerce.user.repository.InvitationTokenRepository;
import com.mkr.commerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository            userRepository;
    private final InvitationTokenRepository invitationTokenRepository;
    private final EmailService              emailService;
    private final AuditLogService           auditLogService;

    @Value("${app.invitation.expiry-hours:48}")
    private int invitationExpiryHours;

    /**
     * Roles each creator level is allowed to assign.
     * A role cannot create peers or superiors.
     */
    private static final Map<UserRole, Set<UserRole>> CREATABLE_BY = Map.of(
        UserRole.SUPER_ADMIN, EnumSet.of(UserRole.ADMIN, UserRole.SALES, UserRole.INVENTORY, UserRole.SUPPORT),
        UserRole.ADMIN,       EnumSet.of(UserRole.ADMIN, UserRole.SALES, UserRole.INVENTORY, UserRole.SUPPORT),
        UserRole.SALES,       EnumSet.of(UserRole.SALES, UserRole.INVENTORY, UserRole.SUPPORT),
        UserRole.INVENTORY,   EnumSet.noneOf(UserRole.class),
        UserRole.SUPPORT,     EnumSet.noneOf(UserRole.class)
    );

    /**
     * Roles visible to each role in the staff list.
     * SUPER_ADMIN and ADMIN see everyone (org-wide visibility).
     * Management of subordinates is still gated by assertCanManage().
     */
    private static final Map<UserRole, Set<UserRole>> VISIBLE_ROLES = Map.of(
        UserRole.SUPER_ADMIN, EnumSet.allOf(UserRole.class),
        UserRole.ADMIN,       EnumSet.allOf(UserRole.class),
        UserRole.SALES,       EnumSet.of(UserRole.SALES, UserRole.INVENTORY, UserRole.SUPPORT),
        UserRole.INVENTORY,   EnumSet.noneOf(UserRole.class),
        UserRole.SUPPORT,     EnumSet.noneOf(UserRole.class)
    );

    // ── Create Staff ──────────────────────────────────────────────────────────

    @Transactional
    public StaffDto createStaff(CreateStaffRequest request, User creator) {
        Set<UserRole> allowed = CREATABLE_BY.getOrDefault(creator.getRole(), Set.of());

        if (!allowed.contains(request.role())) {
            throw new ForbiddenException(
                "You are not permitted to create a " + request.role().name().replace('_', ' ') + " account."
            );
        }

        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(
                "An account with this email already exists.", ErrorCode.DUPLICATE_EMAIL
            );
        }

        // Draw next employee ID from PostgreSQL sequence
        Long empId = userRepository.nextEmployeeId();

        String firstName = request.firstName().trim();
        String lastName  = request.lastName().trim();

        // Create account — no password yet (invitation pending), inactive until set
        User staff = User.builder()
                .employeeId(empId)
                .firstName(firstName)
                .middleName(request.middleName() != null ? request.middleName().trim() : null)
                .lastName(lastName)
                .email(email)
                .role(request.role())
                .department(request.department().trim())
                .mobileNumber(normalizePhone(request.mobileNumber()))
                .alternativePhone(request.alternativePhone() != null ? normalizePhone(request.alternativePhone()) : null)
                .addressBuilding(request.addressBuilding())
                .addressStreet(request.addressStreet())
                .addressCity(request.addressCity())
                .addressState(request.addressState())
                .addressPostalCode(request.addressPostalCode())
                .addressCountry(request.addressCountry())
                .isActive(false)
                .build();
        staff.composeName();
        userRepository.save(staff);

        // Issue invitation token
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        InvitationToken token = InvitationToken.builder()
                .user(staff)
                .token(rawToken)
                .expiresAt(Instant.now().plus(invitationExpiryHours, ChronoUnit.HOURS))
                .build();
        invitationTokenRepository.save(token);

        // Send welcome email (async — does not block response)
        emailService.sendInvitationEmail(email, staff.getName(), creator.getName(), rawToken);

        auditLogService.log(staff.getId(), creator, AuditAction.ACCOUNT_CREATED, "Role: " + request.role().name());
        auditLogService.log(staff.getId(), creator, AuditAction.INVITATION_SENT, null);

        log.info("Staff created: {} [{}] by {} [{}]",
                email, request.role(), creator.getEmail(), creator.getRole());

        return StaffDto.from(staff);
    }

    // ── List Staff ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<StaffDto> listStaff(
            User            viewer,
            UserRole        roleFilter,
            Boolean         activeFilter,
            String          search,
            Pageable        pageable
    ) {
        Set<UserRole> visible = VISIBLE_ROLES.getOrDefault(viewer.getRole(), Set.of());

        // If explicit role filter given, verify the viewer can see that role
        Set<UserRole> roles = (roleFilter != null && visible.contains(roleFilter))
                ? EnumSet.of(roleFilter)
                : visible;

        // SUPER_ADMIN and ADMIN see everyone (org-wide view)
        if (viewer.getRole() == UserRole.SUPER_ADMIN || viewer.getRole() == UserRole.ADMIN) {
            roles = EnumSet.allOf(UserRole.class);
            if (roleFilter != null) roles = EnumSet.of(roleFilter);
        }

        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase().trim() + "%" : null;
        return userRepository.findStaff(roles, activeFilter, searchPattern, pageable)
                .map(StaffDto::from);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StaffDto getStaff(UUID id, User viewer) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        assertCanManage(viewer, staff);
        return StaffDto.from(staff);
    }

    // ── Update Status (activate / deactivate) ─────────────────────────────────

    @Transactional
    public void updateStatus(UUID id, UpdateStatusRequest request, User actor) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        assertCanManage(actor, staff);

        if (staff.getId().equals(actor.getId())) {
            throw new BadRequestException("You cannot deactivate your own account.", ErrorCode.FORBIDDEN);
        }

        staff.setActive(request.active());
        userRepository.save(staff);

        auditLogService.log(staff.getId(), actor,
                request.active() ? AuditAction.ACCOUNT_ACTIVATED : AuditAction.ACCOUNT_DEACTIVATED, null);

        log.info("Staff {} {} by {}",
                staff.getEmail(),
                request.active() ? "activated" : "deactivated",
                actor.getEmail());
    }

    // ── Update Role (SUPER_ADMIN only) ────────────────────────────────────────

    @Transactional
    public void updateRole(UUID id, UpdateRoleRequest request, User actor) {
        if (actor.getRole() != UserRole.SUPER_ADMIN) {
            throw new ForbiddenException("Only Super Admins can change roles.");
        }

        User staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        if (staff.getId().equals(actor.getId())) {
            throw new BadRequestException("You cannot change your own role.", ErrorCode.FORBIDDEN);
        }

        UserRole oldRole = staff.getRole();
        staff.setRole(request.role());
        userRepository.save(staff);

        auditLogService.log(staff.getId(), actor, AuditAction.ROLE_CHANGED,
                oldRole.name() + " → " + request.role().name());

        log.info("Role changed: {} {} → {} by {}",
                staff.getEmail(), oldRole, request.role(), actor.getEmail());
    }

    // ── Update Staff (contact / address / name) ───────────────────────────────

    @Transactional
    public StaffDto updateStaff(UUID id, UpdateStaffRequest request, User actor) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        assertCanManage(actor, staff);

        if (request.firstName()  != null) staff.setFirstName(request.firstName().trim());
        if (request.middleName() != null) staff.setMiddleName(request.middleName().isBlank() ? null : request.middleName().trim());
        if (request.lastName()   != null) staff.setLastName(request.lastName().trim());

        if (request.email() != null) {
            String email = request.email().toLowerCase().trim();
            if (!email.equals(staff.getEmail()) && userRepository.existsByEmail(email)) {
                throw new BadRequestException("An account with this email already exists.", ErrorCode.DUPLICATE_EMAIL);
            }
            staff.setEmail(email);
        }

        if (request.department()      != null) staff.setDepartment(request.department().trim());
        if (request.mobileNumber()    != null) staff.setMobileNumber(normalizePhone(request.mobileNumber()));
        if (request.alternativePhone() != null) staff.setAlternativePhone(request.alternativePhone().isBlank() ? null : normalizePhone(request.alternativePhone()));

        if (request.addressBuilding()   != null) staff.setAddressBuilding(request.addressBuilding().isBlank()   ? null : request.addressBuilding().trim());
        if (request.addressStreet()     != null) staff.setAddressStreet(request.addressStreet().isBlank()       ? null : request.addressStreet().trim());
        if (request.addressCity()       != null) staff.setAddressCity(request.addressCity().isBlank()           ? null : request.addressCity().trim());
        if (request.addressState()      != null) staff.setAddressState(request.addressState().isBlank()         ? null : request.addressState().trim());
        if (request.addressPostalCode() != null) staff.setAddressPostalCode(request.addressPostalCode().isBlank() ? null : request.addressPostalCode().trim());
        if (request.addressCountry()    != null) staff.setAddressCountry(request.addressCountry().isBlank()     ? null : request.addressCountry().trim());

        staff.composeName();
        return StaffDto.from(userRepository.save(staff));
    }

    // ── Resend Invitation ─────────────────────────────────────────────────────

    @Transactional
    public void resendInvitation(UUID id, User actor) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));

        assertCanManage(actor, staff);

        if (staff.getPasswordHash() != null || staff.getGoogleId() != null) {
            throw new BadRequestException(
                "This staff member has already set up their account.", ErrorCode.FORBIDDEN
            );
        }

        // Invalidate previous token and issue a fresh one
        invitationTokenRepository.invalidateAllForUser(staff);

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        invitationTokenRepository.save(InvitationToken.builder()
                .user(staff)
                .token(rawToken)
                .expiresAt(Instant.now().plus(invitationExpiryHours, ChronoUnit.HOURS))
                .build());

        emailService.sendInvitationEmail(staff.getEmail(), staff.getName(), actor.getName(), rawToken);
        auditLogService.log(staff.getId(), actor, AuditAction.INVITATION_RESENT, null);
        log.info("Invitation resent for {} by {}", staff.getEmail(), actor.getEmail());
    }

    // ── Audit Logs ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogs(UUID id, User viewer) {
        userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
        return auditLogService.getLogsForUser(id);
    }

    // ── Uniqueness checks ─────────────────────────────────────────────────────

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    public boolean isPhoneTaken(String phone) {
        return userRepository.existsByMobileNumber(phone.trim());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Asserts that the actor has authority over the given staff member.
     * SUPER_ADMIN can manage everyone. Others can only manage roles they can create.
     */
    private void assertCanManage(User actor, User target) {
        if (actor.getRole() == UserRole.SUPER_ADMIN) return;
        Set<UserRole> manageable = CREATABLE_BY.getOrDefault(actor.getRole(), Set.of());
        if (!manageable.contains(target.getRole())) {
            throw new ForbiddenException("You do not have permission to manage this staff member.");
        }
    }

    private String normalizePhone(String raw) {
        String phone = raw.trim().replaceAll("[\\s\\-()]", "");
        if (phone.startsWith("+91"))                      return phone.substring(3);
        if (phone.startsWith("91") && phone.length() == 12) return phone.substring(2);
        return phone;
    }

    private String buildInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
