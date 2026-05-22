package com.mkr.commerce.user.controller;

import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.user.dto.*;

import java.util.List;
import com.mkr.commerce.user.entity.User;
import com.mkr.commerce.user.enums.UserRole;
import com.mkr.commerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Staff management endpoints.
 *
 * All endpoints require authentication.
 * Fine-grained role checks are enforced in UserService,
 * but broad role gates use @PreAuthorize to return 403 early.
 *
 * Base: /api/users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ── POST /api/users — create new staff member ─────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<StaffDto>> createStaff(
            @Valid @RequestBody CreateStaffRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        StaffDto created = userService.createStaff(request, currentUser);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Staff member created. Invitation email sent to " + created.email(), created));
    }

    // ── GET /api/users — paginated staff list with filters ────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<Page<StaffDto>>> listStaff(
            @RequestParam(required = false) UserRole  role,
            @RequestParam(required = false) Boolean   active,
            @RequestParam(required = false) String    search,
            @RequestParam(defaultValue = "0")  int   page,
            @RequestParam(defaultValue = "20") int   size,
            @AuthenticationPrincipal User currentUser
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StaffDto> result = userService.listStaff(currentUser, role, active, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Staff list", result));
    }

    // ── GET /api/users/{id} — single staff member ─────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<StaffDto>> getStaff(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Staff member", userService.getStaff(id, currentUser)));
    }

    // ── PATCH /api/users/{id} — update contact / address / name ─────────────

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<StaffDto>> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        StaffDto updated = userService.updateStaff(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Staff member updated.", updated));
    }

    // ── PATCH /api/users/{id}/status — activate or deactivate ────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.updateStatus(id, request, currentUser);
        String action = Boolean.TRUE.equals(request.active()) ? "activated" : "deactivated";
        return ResponseEntity.ok(ApiResponse.ok("Staff member " + action + " successfully."));
    }

    // ── PATCH /api/users/{id}/role — change role (SUPER_ADMIN only) ──────────

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.updateRole(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Role updated successfully."));
    }

    // ── POST /api/users/{id}/resend-invitation ────────────────────────────────

    @PostMapping("/{id}/resend-invitation")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<Void>> resendInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        userService.resendInvitation(id, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Invitation email resent."));
    }

    // ── GET /api/users/{id}/audit-logs ────────────────────────────────────────

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAuditLogs(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Activity log", userService.getAuditLogs(id, currentUser)));
    }
}
