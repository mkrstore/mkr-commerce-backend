package com.mkr.commerce.catalog.service;

import com.mkr.commerce.catalog.dto.specgroup.*;
import com.mkr.commerce.catalog.entity.*;
import com.mkr.commerce.catalog.repository.*;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecGroupService {

    private final SpecGroupRepository          specGroupRepo;
    private final LookupListRepository         lookupListRepo;
    private final CategoryRepository           categoryRepo;
    private final CategorySpecGroupRepository  catSpecGroupRepo;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SpecGroupSummaryDto> listAll() {
        return specGroupRepo.findAllByOrderByNameAsc().stream()
                .map(SpecGroupSummaryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SpecGroupDto getById(UUID id) {
        return SpecGroupDto.from(findById(id));
    }

    @Transactional
    public SpecGroupDto create(SaveSpecGroupRequest req) {
        if (specGroupRepo.existsByNameIgnoreCase(req.name().trim())) {
            throw new BadRequestException("A spec group named '" + req.name() + "' already exists.");
        }
        SpecGroup group = SpecGroup.builder()
                .name(req.name().trim())
                .description(req.description() != null ? req.description().trim() : null)
                .build();
        group = specGroupRepo.save(group);
        syncFields(group, req.fieldIds());
        log.info("SpecGroup created: '{}' [{}]", group.getName(), group.getId());
        return SpecGroupDto.from(specGroupRepo.findById(group.getId()).orElseThrow());
    }

    @Transactional
    public SpecGroupDto update(UUID id, SaveSpecGroupRequest req) {
        SpecGroup group = findById(id);
        if (specGroupRepo.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id)) {
            throw new BadRequestException("A spec group named '" + req.name() + "' already exists.");
        }
        group.setName(req.name().trim());
        group.setDescription(req.description() != null ? req.description().trim() : null);
        syncFields(group, req.fieldIds());
        log.info("SpecGroup updated: '{}' [{}]", group.getName(), id);
        return SpecGroupDto.from(specGroupRepo.findById(id).orElseThrow());
    }

    @Transactional
    public void delete(UUID id) {
        SpecGroup group = findById(id);
        specGroupRepo.delete(group);
        log.info("SpecGroup deleted: '{}' [{}]", group.getName(), id);
    }

    // ── Category assignment ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SpecGroupSummaryDto> listForCategory(UUID categoryId) {
        if (!categoryRepo.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        return catSpecGroupRepo.findByCategory_IdOrderBySortOrderAsc(categoryId).stream()
                .map(csg -> SpecGroupSummaryDto.from(csg.getGroup()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignToCategory(UUID categoryId, UUID groupId) {
        Category category = categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        SpecGroup group = findById(groupId);
        if (catSpecGroupRepo.existsByCategory_IdAndGroup_Id(categoryId, groupId)) {
            throw new BadRequestException("This spec group is already assigned to this category.");
        }
        int sortOrder = catSpecGroupRepo.countByCategory_Id(categoryId);
        CategorySpecGroup csg = CategorySpecGroup.builder()
                .category(category)
                .group(group)
                .sortOrder(sortOrder)
                .build();
        catSpecGroupRepo.save(csg);
        log.info("SpecGroup '{}' assigned to category [{}]", group.getName(), categoryId);
    }

    @Transactional
    public void removeFromCategory(UUID categoryId, UUID groupId) {
        CategorySpecGroup csg = catSpecGroupRepo.findByCategory_IdAndGroup_Id(categoryId, groupId)
                .orElseThrow(() -> new BadRequestException("This spec group is not assigned to this category."));
        catSpecGroupRepo.delete(csg);
        log.info("SpecGroup [{}] removed from category [{}]", groupId, categoryId);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private SpecGroup findById(UUID id) {
        return specGroupRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spec group not found: " + id));
    }

    private void syncFields(SpecGroup group, List<UUID> fieldIds) {
        group.getFields().clear();
        Set<UUID> seen = new LinkedHashSet<>();
        int order = 0;
        for (UUID fieldId : fieldIds) {
            if (!seen.add(fieldId)) continue;
            LookupList field = lookupListRepo.findById(fieldId)
                    .orElseThrow(() -> new BadRequestException("Custom field not found: " + fieldId));
            group.getFields().add(SpecGroupField.builder()
                    .group(group)
                    .field(field)
                    .sortOrder(order++)
                    .build());
        }
        specGroupRepo.save(group);
    }
}
