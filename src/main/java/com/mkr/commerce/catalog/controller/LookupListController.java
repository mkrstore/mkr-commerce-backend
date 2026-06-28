package com.mkr.commerce.catalog.controller;

import com.mkr.commerce.catalog.dto.lookup.LookupListDto;
import com.mkr.commerce.catalog.dto.lookup.LookupListSummaryDto;
import com.mkr.commerce.catalog.dto.lookup.SaveLookupListRequest;
import com.mkr.commerce.catalog.service.LookupListService;
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
@RequestMapping("/api/lookup-lists")
@RequiredArgsConstructor
public class LookupListController {

    private final LookupListService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<PageDto<LookupListSummaryDto>>> list(
            @RequestParam(defaultValue = "")  String search,
            @RequestParam(defaultValue = "0") int    page,
            @RequestParam(defaultValue = "16") int   size) {
        return ResponseEntity.ok(ApiResponse.ok("Fields", service.listPaged(search, page, size)));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<List<LookupListSummaryDto>>> listAll() {
        return ResponseEntity.ok(ApiResponse.ok("All fields", service.listAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<LookupListDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Lookup list", service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<LookupListDto>> create(@Valid @RequestBody SaveLookupListRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Created", service.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<LookupListDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody SaveLookupListRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Updated", service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted"));
    }
}
