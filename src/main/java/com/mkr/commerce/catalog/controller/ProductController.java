package com.mkr.commerce.catalog.controller;

import com.mkr.commerce.catalog.dto.product.*;
import com.mkr.commerce.catalog.enums.MediaType;
import com.mkr.commerce.catalog.enums.ProductStatus;
import com.mkr.commerce.catalog.service.ProductService;
import com.mkr.commerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ── GET /api/products ─────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductSummaryDto>>> list(
            @RequestParam(required = false) UUID          categoryId,
            @RequestParam(required = false) UUID          brandId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) String        search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc")       String dir
    ) {
        Sort sort = dir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 500), sort);
        return ResponseEntity.ok(ApiResponse.ok("Products",
                productService.list(categoryId, brandId, status, search, pageable)));
    }

    // ── GET /api/products/{id} ────────────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Product", productService.get(id)));
    }

    // ── POST /api/products ────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<ProductDetailDto>> create(@Valid @RequestBody CreateProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", productService.create(req)));
    }

    // ── PATCH /api/products/{id} ──────────────────────────────────────────────

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<ProductDetailDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Product updated", productService.update(id, req)));
    }

    // ── DELETE /api/products/{id} ─────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted"));
    }

    // ── PUT /api/products/{id}/attributes ─────────────────────────────────────

    @PutMapping("/{id}/attributes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<List<ProductAttributeDto>>> saveAttributes(
            @PathVariable UUID id,
            @Valid @RequestBody SaveAttributesRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Attributes saved", productService.saveAttributes(id, req)));
    }

    // ── POST /api/products/{id}/media ─────────────────────────────────────────

    @PostMapping(value = "/{id}/media", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<ProductImageDto>> uploadMedia(
            @PathVariable UUID id,
            @RequestParam("file")                               MultipartFile file,
            @RequestParam(value = "mediaType", required = false) MediaType    mediaType,
            @RequestParam(value = "altText",   required = false) String       altText
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Media uploaded", productService.uploadMedia(id, file, mediaType, altText)));
    }

    // ── DELETE /api/products/{id}/media/{mediaId} ─────────────────────────────

    @DeleteMapping("/{id}/media/{mediaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable UUID id,
            @PathVariable UUID mediaId
    ) {
        productService.deleteMedia(id, mediaId);
        return ResponseEntity.ok(ApiResponse.ok("Media deleted"));
    }

    // ── POST /api/products/{id}/clone ─────────────────────────────────────────

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<ProductDetailDto>> clone(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product cloned", productService.clone(id)));
    }

    // ── POST /api/products/{id}/variants ──────────────────────────────────────

    @PostMapping("/{id}/variants")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> addVariant(
            @PathVariable UUID id,
            @Valid @RequestBody CreateVariantRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Variant added", productService.addVariant(id, req)));
    }

    // ── PUT /api/products/{id}/variants/{variantId} ───────────────────────────

    @PutMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<ProductVariantDto>> updateVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateVariantRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Variant updated",
                productService.updateVariant(id, variantId, req)));
    }

    // ── DELETE /api/products/{id}/variants/{variantId} ────────────────────────

    @DeleteMapping("/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable UUID id,
            @PathVariable UUID variantId
    ) {
        productService.deleteVariant(id, variantId);
        return ResponseEntity.ok(ApiResponse.ok("Variant deleted"));
    }
}
