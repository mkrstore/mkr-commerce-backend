package com.mkr.commerce.catalog.service;

import com.mkr.commerce.catalog.dto.category.*;
import com.mkr.commerce.catalog.entity.AttributeDefinition;
import com.mkr.commerce.catalog.entity.Category;
import com.mkr.commerce.catalog.repository.AttributeDefinitionRepository;
import com.mkr.commerce.catalog.repository.CategoryRepository;
import com.mkr.commerce.catalog.repository.ProductRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.common.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository          categoryRepo;
    private final ProductRepository           productRepo;
    private final AttributeDefinitionRepository attrDefRepo;
    private final CloudinaryService            cloudinary;

    // ── List (paginated flat) ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CategoryDto> list(Boolean active, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return categoryRepo.searchActive(search, pageable).map(CategoryDto::from);
        }
        if (active != null) {
            return categoryRepo.findAllByIsActive(active, pageable).map(CategoryDto::from);
        }
        return categoryRepo.findAll(pageable).map(CategoryDto::from);
    }

    // ── Tree (nested, active only) ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryTreeDto> tree() {
        return categoryRepo.findAllByParentIsNullAndIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(CategoryTreeDto::from)
                .toList();
    }

    // ── Get single ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CategoryDto get(UUID id) {
        return CategoryDto.from(findById(id));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public CategoryDto create(CreateCategoryRequest req) {
        String slug = resolveSlug(req.slug(), req.name());
        if (categoryRepo.existsBySlug(slug)) {
            throw new BadRequestException("A category with slug '" + slug + "' already exists.");
        }

        Category parent = req.parentId() != null ? findById(req.parentId()) : null;

        Category category = Category.builder()
                .name(req.name().trim())
                .slug(slug)
                .description(req.description())
                .parent(parent)
                .sortOrder(req.sortOrder())
                .isActive(true)
                .build();

        Category saved = categoryRepo.save(category);
        log.info("Category created: '{}' [{}]", saved.getName(), saved.getId());
        return CategoryDto.from(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public CategoryDto update(UUID id, UpdateCategoryRequest req) {
        Category category = findById(id);

        String slug = resolveSlug(req.slug(), req.name());
        if (categoryRepo.existsBySlugAndIdNot(slug, id)) {
            throw new BadRequestException("A category with slug '" + slug + "' already exists.");
        }
        if (categoryRepo.existsByNameAndIdNot(req.name().trim(), id)) {
            throw new BadRequestException("A category named '" + req.name() + "' already exists.");
        }

        // Prevent setting itself or descendant as parent
        if (req.parentId() != null) {
            if (req.parentId().equals(id)) throw new BadRequestException("A category cannot be its own parent.");
        }

        Category parent = req.parentId() != null ? findById(req.parentId()) : null;

        category.setName(req.name().trim());
        category.setSlug(slug);
        category.setDescription(req.description());
        category.setParent(parent);
        category.setSortOrder(req.sortOrder());
        category.setActive(req.isActive());

        Category saved = categoryRepo.save(category);
        log.info("Category updated: '{}' [{}]", saved.getName(), id);
        return CategoryDto.from(saved);
    }

    // ── Delete (deactivate) ───────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id) {
        Category category = findById(id);
        if (categoryRepo.existsByParentId(id)) {
            throw new BadRequestException("Cannot delete a category that has sub-categories. Remove sub-categories first.");
        }
        if (productRepo.existsByCategoryId(id)) {
            throw new BadRequestException("Cannot delete a category that has products assigned to it.");
        }
        category.setActive(false);
        categoryRepo.save(category);
        log.info("Category deactivated: '{}' [{}]", category.getName(), id);
    }

    // ── Attribute Definitions ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AttributeDefinitionDto> listAttributes(UUID categoryId) {
        findById(categoryId);
        return attrDefRepo.findAllByCategoryIdOrderBySortOrderAsc(categoryId)
                .stream().map(AttributeDefinitionDto::from).toList();
    }

    @Transactional
    public AttributeDefinitionDto createAttribute(UUID categoryId, CreateAttributeDefinitionRequest req) {
        Category category = findById(categoryId);

        String key = req.fieldKey().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (attrDefRepo.existsByFieldKeyAndCategoryId(key, categoryId)) {
            throw new BadRequestException("An attribute with key '" + key + "' already exists for this category.");
        }

        AttributeDefinition def = AttributeDefinition.builder()
                .category(category)
                .label(req.label().trim())
                .fieldKey(key)
                .fieldType(req.fieldType())
                .options(req.options())
                .unit(req.unit())
                .defaultValue(req.defaultValue())
                .required(req.required())
                .sortOrder(req.sortOrder())
                .build();

        AttributeDefinition saved = attrDefRepo.save(def);
        log.info("Attribute '{}' added to category [{}]", saved.getFieldKey(), categoryId);
        return AttributeDefinitionDto.from(saved);
    }

    @Transactional
    public AttributeDefinitionDto updateAttribute(UUID categoryId, UUID attrId, UpdateAttributeDefinitionRequest req) {
        findById(categoryId);
        AttributeDefinition def = attrDefRepo.findByIdAndCategoryId(attrId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found."));

        def.setLabel(req.label().trim());
        def.setFieldType(req.fieldType());
        def.setOptions(req.options());
        def.setUnit(req.unit());
        def.setDefaultValue(req.defaultValue());
        def.setRequired(req.required());
        def.setSortOrder(req.sortOrder());

        return AttributeDefinitionDto.from(attrDefRepo.save(def));
    }

    @Transactional
    public void deleteAttribute(UUID categoryId, UUID attrId) {
        findById(categoryId);
        AttributeDefinition def = attrDefRepo.findByIdAndCategoryId(attrId, categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Attribute definition not found."));
        attrDefRepo.delete(def);
        log.info("Attribute '{}' deleted from category [{}]", def.getFieldKey(), categoryId);
    }

    // ── Image / Video ─────────────────────────────────────────────────────────

    @Transactional
    public CategoryDto uploadImage(UUID id, MultipartFile file, boolean isVideo) {
        Category cat = findById(id);
        if (cat.getImagePublicId() != null) {
            cloudinary.delete(cat.getImagePublicId());
        }
        CloudinaryService.UploadResult result = cloudinary.upload(file, "mkr-commerce/categories", isVideo);
        cat.setImageUrl(result.url());
        cat.setImagePublicId(result.publicId());
        cat.setImageIsVideo(isVideo);
        log.info("Image uploaded for category [{}]: {}", id, result.publicId());
        return CategoryDto.from(categoryRepo.save(cat));
    }

    @Transactional
    public CategoryDto removeImage(UUID id) {
        Category cat = findById(id);
        if (cat.getImagePublicId() != null) {
            cloudinary.delete(cat.getImagePublicId());
        }
        cat.setImageUrl(null);
        cat.setImagePublicId(null);
        cat.setImageIsVideo(false);
        log.info("Image removed for category [{}]", id);
        return CategoryDto.from(categoryRepo.save(cat));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Category findById(UUID id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private String resolveSlug(String rawSlug, String name) {
        String slug = (rawSlug != null && !rawSlug.isBlank()) ? rawSlug : SlugUtils.toSlug(name);
        if (slug == null || slug.isBlank()) throw new BadRequestException("Could not generate a valid slug.");
        return slug;
    }
}
