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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository          categoryRepo;
    private final ProductRepository           productRepo;
    private final AttributeDefinitionRepository attrDefRepo;

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

        return CategoryDto.from(categoryRepo.save(category));
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

        return CategoryDto.from(categoryRepo.save(category));
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

        return AttributeDefinitionDto.from(attrDefRepo.save(def));
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
