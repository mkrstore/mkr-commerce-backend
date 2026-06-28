package com.mkr.commerce.catalog.service;

import com.mkr.commerce.catalog.dto.lookup.LookupListDto;
import com.mkr.commerce.catalog.dto.lookup.LookupListSummaryDto;
import com.mkr.commerce.catalog.dto.lookup.SaveLookupListRequest;
import com.mkr.commerce.catalog.entity.LookupList;
import com.mkr.commerce.catalog.entity.LookupValue;
import com.mkr.commerce.catalog.repository.LookupListRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.common.response.PageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LookupListService {

    private final LookupListRepository repo;

    public List<LookupListSummaryDto> listAll() {
        return repo.findAllByOrderByNameAsc().stream().map(LookupListSummaryDto::from).toList();
    }

    public PageDto<LookupListSummaryDto> listPaged(String search, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var result   = repo.findByNameContainingIgnoreCaseOrderByNameAsc(
                search == null ? "" : search.trim(), pageable);
        return PageDto.of(result.map(LookupListSummaryDto::from));
    }

    public LookupListDto getById(UUID id) {
        return LookupListDto.from(findOrThrow(id));
    }

    @Transactional
    public LookupListDto create(SaveLookupListRequest req) {
        if (repo.existsByNameIgnoreCase(req.name().trim()))
            throw new BadRequestException("A lookup list named '" + req.name() + "' already exists.");

        LookupList list = LookupList.builder()
                .name(req.name().trim())
                .description(req.description() != null ? req.description().trim() : null)
                .fieldType(req.fieldType() != null ? req.fieldType().trim() : "SELECT")
                .required(req.required())
                .defaultValue(req.defaultValue() != null ? req.defaultValue().trim() : null)
                .build();
        populateValues(list, req.values());
        LookupList saved = repo.save(list);
        log.info("Lookup list '{}' created with {} values", saved.getName(), saved.getValues().size());
        return LookupListDto.from(saved);
    }

    @Transactional
    public LookupListDto update(UUID id, SaveLookupListRequest req) {
        LookupList list = findOrThrow(id);
        if (repo.existsByNameIgnoreCaseAndIdNot(req.name().trim(), id))
            throw new BadRequestException("A lookup list named '" + req.name() + "' already exists.");

        list.setName(req.name().trim());
        list.setDescription(req.description() != null ? req.description().trim() : null);
        list.setFieldType(req.fieldType() != null ? req.fieldType().trim() : "SELECT");
        list.setRequired(req.required());
        list.setDefaultValue(req.defaultValue() != null ? req.defaultValue().trim() : null);
        list.getValues().clear();
        populateValues(list, req.values());
        log.info("Lookup list '{}' updated with {} values", list.getName(), list.getValues().size());
        return LookupListDto.from(repo.save(list));
    }

    @Transactional
    public void delete(UUID id) {
        LookupList list = findOrThrow(id);
        repo.delete(list);
        log.info("Lookup list '{}' deleted", list.getName());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LookupList findOrThrow(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lookup list not found."));
    }

    private void populateValues(LookupList list, List<String> values) {
        if (values == null) return;
        for (int i = 0; i < values.size(); i++) {
            String v = values.get(i).trim();
            if (!v.isEmpty())
                list.getValues().add(LookupValue.builder().lookupList(list).value(v).sortOrder(i).build());
        }
    }
}
