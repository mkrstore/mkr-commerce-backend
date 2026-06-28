package com.mkr.commerce.vendor.controller;

import com.mkr.commerce.catalog.dto.product.ProductSummaryDto;
import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.inventory.dto.InventoryTransactionDto;
import com.mkr.commerce.vendor.dto.*;
import com.mkr.commerce.vendor.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<List<VendorDto>>> list(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Vendors", vendorService.listAll(search)));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY','SALES')")
    public ResponseEntity<ApiResponse<List<VendorSummaryDto>>> summary() {
        return ResponseEntity.ok(ApiResponse.ok("Vendors", vendorService.listSummary()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<VendorDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor", vendorService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<VendorDto>> create(@Valid @RequestBody CreateVendorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Vendor created", vendorService.create(req)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<VendorDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVendorRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Vendor updated", vendorService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        vendorService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.ok("Vendor deactivated"));
    }

    @GetMapping("/{id}/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getProducts(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Products", vendorService.getProducts(id)));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<Page<InventoryTransactionDto>>> getTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Transactions", vendorService.getTransactions(id, page, size)));
    }
}
