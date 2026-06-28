package com.mkr.commerce.catalog.controller;

import com.mkr.commerce.catalog.dto.specgroup.*;
import com.mkr.commerce.catalog.service.SpecGroupService;
import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.common.response.PageDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spec-groups")
@RequiredArgsConstructor
public class SpecGroupController {

    private final SpecGroupService service;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<PageDto<SpecGroupSummaryDto>>> list(
            @RequestParam(defaultValue = "")   String search,
            @RequestParam(defaultValue = "0")  int    page,
            @RequestParam(defaultValue = "16") int    size) {
        return ResponseEntity.ok(ApiResponse.ok("Spec groups", service.listPaged(search, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<SpecGroupDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Spec group", service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<SpecGroupDto>> create(@Valid @RequestBody SaveSpecGroupRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Spec group created", service.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<SpecGroupDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveSpecGroupRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Spec group updated", service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Spec group deleted"));
    }

    // ── Category assignment ───────────────────────────────────────────────────

    @GetMapping("/by-category/{categoryId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<List<SpecGroupSummaryDto>>> listByCategory(
            @PathVariable UUID categoryId) {
        return ResponseEntity.ok(ApiResponse.ok("Spec groups for category", service.listForCategory(categoryId)));
    }

    @PostMapping("/by-category/{categoryId}/{groupId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignToCategory(
            @PathVariable UUID categoryId,
            @PathVariable UUID groupId) {
        service.assignToCategory(categoryId, groupId);
        return ResponseEntity.ok(ApiResponse.ok("Spec group assigned to category"));
    }

    @DeleteMapping("/by-category/{categoryId}/{groupId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeFromCategory(
            @PathVariable UUID categoryId,
            @PathVariable UUID groupId) {
        service.removeFromCategory(categoryId, groupId);
        return ResponseEntity.ok(ApiResponse.ok("Spec group removed from category"));
    }
}
