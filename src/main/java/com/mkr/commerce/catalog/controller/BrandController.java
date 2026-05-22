package com.mkr.commerce.catalog.controller;

import com.mkr.commerce.catalog.dto.brand.*;
import com.mkr.commerce.catalog.service.BrandService;
import com.mkr.commerce.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BrandDto>>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String  search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok("Brands", brandService.list(active, search, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Brand", brandService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<BrandDto>> create(@Valid @RequestBody CreateBrandRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Brand created", brandService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<BrandDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBrandRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Brand updated", brandService.update(id, req)));
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<BrandDto>> uploadLogo(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Logo uploaded", brandService.uploadLogo(id, file, "video".equalsIgnoreCase(type))));
    }

    @DeleteMapping("/{id}/logo")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<BrandDto>> removeLogo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Logo removed", brandService.removeLogo(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        brandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Brand deactivated"));
    }
}
