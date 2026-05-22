package com.mkr.commerce.catalog.controller;

import com.mkr.commerce.catalog.dto.category.*;
import com.mkr.commerce.catalog.service.CategoryService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CategoryDto>>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String  search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("sortOrder").ascending().and(Sort.by("name")));
        return ResponseEntity.ok(ApiResponse.ok("Categories", categoryService.list(active, search, pageable)));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryTreeDto>>> tree() {
        return ResponseEntity.ok(ApiResponse.ok("Category tree", categoryService.tree()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Category", categoryService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<CategoryDto>> create(@Valid @RequestBody CreateCategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created", categoryService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<CategoryDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategoryRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Category updated", categoryService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deactivated"));
    }

    // ── Attribute Definitions ─────────────────────────────────────────────────

    @GetMapping("/{id}/attributes")
    public ResponseEntity<ApiResponse<List<AttributeDefinitionDto>>> listAttributes(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Attributes", categoryService.listAttributes(id)));
    }

    @PostMapping("/{id}/attributes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<AttributeDefinitionDto>> createAttribute(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAttributeDefinitionRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Attribute created", categoryService.createAttribute(id, req)));
    }

    @PutMapping("/{id}/attributes/{attrId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<AttributeDefinitionDto>> updateAttribute(
            @PathVariable UUID id,
            @PathVariable UUID attrId,
            @Valid @RequestBody UpdateAttributeDefinitionRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Attribute updated", categoryService.updateAttribute(id, attrId, req)));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<CategoryDto>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "image") String type
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Image uploaded",
                categoryService.uploadImage(id, file, "video".equalsIgnoreCase(type))));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','INVENTORY')")
    public ResponseEntity<ApiResponse<CategoryDto>> removeImage(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Image removed", categoryService.removeImage(id)));
    }

    @DeleteMapping("/{id}/attributes/{attrId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAttribute(
            @PathVariable UUID id,
            @PathVariable UUID attrId
    ) {
        categoryService.deleteAttribute(id, attrId);
        return ResponseEntity.ok(ApiResponse.ok("Attribute deleted"));
    }
}
