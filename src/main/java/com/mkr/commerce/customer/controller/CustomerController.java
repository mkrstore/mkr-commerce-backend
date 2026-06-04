package com.mkr.commerce.customer.controller;

import com.mkr.commerce.billing.dto.CustomerBillSummaryDto;
import com.mkr.commerce.billing.service.BillingService;
import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.customer.dto.CustomerDetailDto;
import com.mkr.commerce.customer.dto.CustomerSummaryDto;
import com.mkr.commerce.customer.dto.CreateCustomerRequest;
import com.mkr.commerce.customer.dto.UpdateCustomerRequest;
import com.mkr.commerce.customer.dto.UpdateCustomerStatusRequest;
import com.mkr.commerce.customer.dto.UpdateCustomerTypeRequest;
import com.mkr.commerce.customer.enums.CustomerType;
import com.mkr.commerce.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final BillingService  billingService;

    // ── POST /api/customers ───────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> create(
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Customer created", customerService.create(request)));
    }

    // ── GET /api/customers ────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerSummaryDto>>> list(
            @RequestParam(required = false) CustomerType type,
            @RequestParam(defaultValue = "false") boolean pendingOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")      String dir
    ) {
        Sort sort = dir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(ApiResponse.ok("Customers",
                customerService.list(type, pendingOnly, search, pageable)));
    }

    // ── GET /api/customers/{id} ───────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Customer", customerService.getById(id)));
    }

    // ── PATCH /api/customers/{id} — full detail update ───────────────────────

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Customer updated",
                customerService.update(id, request)));
    }

    // ── PATCH /api/customers/{id}/type ───────────────────────────────────────

    @PatchMapping("/{id}/type")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','SALES')")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> updateType(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerTypeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Customer type updated",
                customerService.updateType(id, request.type())));
    }

    // ── GET /api/customers/{id}/bills ────────────────────────────────────────

    @GetMapping("/{id}/bills")
    public ResponseEntity<ApiResponse<List<CustomerBillSummaryDto>>> getBills(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Bills", billingService.getByCustomer(id)));
    }

    // ── PATCH /api/customers/{id}/status ─────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateCustomerStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                request.active() ? "Customer activated" : "Customer deactivated",
                customerService.updateStatus(id, request.active())));
    }
}
