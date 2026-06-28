package com.mkr.commerce.inventory.controller;

import com.mkr.commerce.catalog.dto.product.ProductSummaryDto;
import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.inventory.dto.InventoryTransactionDto;
import com.mkr.commerce.inventory.dto.RestockRequest;
import com.mkr.commerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/{productId}/restock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<Void>> restock(
            @PathVariable UUID productId,
            @Valid @RequestBody RestockRequest req,
            Authentication auth
    ) {
        inventoryService.restock(productId, req, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok("Stock updated"));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<Page<InventoryTransactionDto>>> getTransactions(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Transactions", inventoryService.getTransactions(page, size)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getLowStock(
            @RequestParam(defaultValue = "5") int threshold
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Low stock products", inventoryService.getLowStock(threshold)));
    }
}
