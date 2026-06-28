package com.mkr.commerce.inventory.service;

import com.mkr.commerce.catalog.dto.product.ProductSummaryDto;
import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.entity.ProductVariant;
import com.mkr.commerce.catalog.enums.ProductStatus;
import com.mkr.commerce.catalog.repository.ProductRepository;
import com.mkr.commerce.catalog.repository.ProductVariantRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.vendor.entity.Vendor;
import com.mkr.commerce.vendor.repository.VendorRepository;
import com.mkr.commerce.inventory.dto.InventoryTransactionDto;
import com.mkr.commerce.inventory.dto.RestockRequest;
import com.mkr.commerce.inventory.entity.InventoryTransaction;
import com.mkr.commerce.inventory.enums.TransactionType;
import com.mkr.commerce.inventory.repository.InventoryTransactionRepository;
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
public class InventoryService {

    private final ProductRepository             productRepository;
    private final ProductVariantRepository      variantRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final VendorRepository              vendorRepository;

    @Transactional
    public void restock(UUID productId, RestockRequest req, String userEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        ProductVariant variant = null;
        if (req.variantId() != null) {
            variant = variantRepository.findById(req.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + req.variantId()));
            if (!variant.getProduct().getId().equals(productId)) {
                throw new BadRequestException("Variant does not belong to this product");
            }
            variant.setStockQty(variant.getStockQty() + req.qty());
            variantRepository.save(variant);
        }

        product.setStockQty(product.getStockQty() + req.qty());
        productRepository.save(product);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setProduct(product);
        tx.setVariant(variant);
        tx.setType(TransactionType.IN);
        tx.setQty(req.qty());
        if (req.vendorId() != null) {
            Vendor vendor = vendorRepository.findById(req.vendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + req.vendorId()));
            tx.setVendor(vendor);
            tx.setVendorName(vendor.getName());
        } else {
            tx.setVendorName(req.vendorName() != null && !req.vendorName().isBlank() ? req.vendorName().strip() : null);
        }
        tx.setPurchasePricePerUnit(req.purchasePricePerUnit());
        tx.setNotes(req.notes() != null && !req.notes().isBlank() ? req.notes().strip() : null);
        tx.setCreatedByEmail(userEmail);
        transactionRepository.save(tx);
    }

    public Page<InventoryTransactionDto> getTransactions(int page, int size) {
        return transactionRepository
                .findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))
                .map(InventoryTransactionDto::from);
    }

    public List<ProductSummaryDto> getLowStock(int threshold) {
        return productRepository
                .findByStockQtyLessThanEqualAndStatusOrderByStockQtyAscNameAsc(threshold, ProductStatus.ACTIVE)
                .stream()
                .map(ProductSummaryDto::from)
                .toList();
    }
}
