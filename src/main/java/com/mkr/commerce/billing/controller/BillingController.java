package com.mkr.commerce.billing.controller;

import com.mkr.commerce.billing.dto.BillConfirmResponse;
import com.mkr.commerce.billing.dto.BillingConfirmRequest;
import com.mkr.commerce.billing.service.BillingService;
import com.mkr.commerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','INVENTORY')")
    public ResponseEntity<ApiResponse<Page<BillConfirmResponse>>> list(
            @RequestParam(required = false)         String search,
            @RequestParam(defaultValue = "0")       int page,
            @RequestParam(defaultValue = "50")      int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Bills",
                billingService.list(search, page, Math.min(size, 200))));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','INVENTORY')")
    public ResponseEntity<ApiResponse<BillConfirmResponse>> confirm(
            @Valid @RequestBody BillingConfirmRequest request
    ) {
        BillConfirmResponse response = billingService.confirm(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Bill created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES','INVENTORY')")
    public ResponseEntity<ApiResponse<BillConfirmResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Bill retrieved", billingService.getById(id)));
    }
}
