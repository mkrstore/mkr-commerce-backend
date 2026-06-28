package com.mkr.commerce.vendor.service;

import com.mkr.commerce.catalog.dto.product.ProductSummaryDto;
import com.mkr.commerce.catalog.repository.ProductRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.inventory.dto.InventoryTransactionDto;
import com.mkr.commerce.inventory.repository.InventoryTransactionRepository;
import com.mkr.commerce.vendor.dto.*;
import com.mkr.commerce.vendor.entity.Vendor;
import com.mkr.commerce.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository              vendorRepository;
    private final ProductRepository             productRepository;
    private final InventoryTransactionRepository transactionRepository;

    public List<VendorDto> listAll(String search) {
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.toLowerCase().trim() + "%";
            return vendorRepository.findFiltered(pattern, PageRequest.of(0, 200, Sort.by("name")))
                    .map(VendorDto::from).getContent();
        }
        return vendorRepository.findAllByOrderByNameAsc().stream().map(VendorDto::from).toList();
    }

    public List<VendorSummaryDto> listSummary() {
        return vendorRepository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(VendorSummaryDto::from).toList();
    }

    public VendorDto get(UUID id) {
        return VendorDto.from(findById(id));
    }

    @Transactional
    public VendorDto create(CreateVendorRequest req) {
        Vendor vendor = Vendor.builder()
                .name(req.name().strip())
                .phone(blankToNull(req.phone()))
                .email(blankToNull(req.email()))
                .address(blankToNull(req.address()))
                .gstin(blankToNull(req.gstin()))
                .notes(blankToNull(req.notes()))
                .build();
        return VendorDto.from(vendorRepository.save(vendor));
    }

    @Transactional
    public VendorDto update(UUID id, UpdateVendorRequest req) {
        Vendor vendor = findById(id);
        if (req.name()     != null) vendor.setName(req.name().strip());
        if (req.phone()    != null) vendor.setPhone(blankToNull(req.phone()));
        if (req.email()    != null) vendor.setEmail(blankToNull(req.email()));
        if (req.address()  != null) vendor.setAddress(blankToNull(req.address()));
        if (req.gstin()    != null) vendor.setGstin(blankToNull(req.gstin()));
        if (req.notes()    != null) vendor.setNotes(blankToNull(req.notes()));
        if (req.isActive() != null) vendor.setActive(req.isActive());
        return VendorDto.from(vendorRepository.save(vendor));
    }

    @Transactional
    public void deactivate(UUID id) {
        Vendor vendor = findById(id);
        vendor.setActive(false);
        vendorRepository.save(vendor);
    }

    public List<ProductSummaryDto> getProducts(UUID vendorId) {
        findById(vendorId);
        return productRepository.findByPreferredVendorIdOrderByNameAsc(vendorId)
                .stream().map(ProductSummaryDto::from).toList();
    }

    public Page<InventoryTransactionDto> getTransactions(UUID vendorId, int page, int size) {
        findById(vendorId);
        return transactionRepository.findByVendorId(
                vendorId, PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending())
        ).map(InventoryTransactionDto::from);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Vendor findById(UUID id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
