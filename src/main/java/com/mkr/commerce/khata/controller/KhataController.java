package com.mkr.commerce.khata.controller;

import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.khata.dto.AddKhataEntryRequest;
import com.mkr.commerce.khata.dto.CollectPaymentRequest;
import com.mkr.commerce.khata.dto.KhataEntryDto;
import com.mkr.commerce.khata.dto.KhataPageDto;
import com.mkr.commerce.khata.service.KhataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/customers/{customerId}/khata")
@RequiredArgsConstructor
public class KhataController {

    private final KhataService khataService;

    // ── GET /api/customers/{id}/khata ─────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<KhataPageDto>> getEntries(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Khata entries", khataService.getEntries(customerId)));
    }

    // ── POST /api/customers/{id}/khata ────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<KhataEntryDto>> addEntry(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddKhataEntryRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Khata entry added", khataService.addEntry(customerId, request)));
    }

    // ── POST /api/customers/{id}/khata/collect ────────────────────────────────

    @PostMapping("/collect")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<KhataEntryDto>> collectPayment(
            @PathVariable UUID customerId,
            @Valid @RequestBody CollectPaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Payment collected", khataService.collectPayment(customerId, request)));
    }
}
