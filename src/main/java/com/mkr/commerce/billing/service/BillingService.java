package com.mkr.commerce.billing.service;

import com.mkr.commerce.billing.dto.BillConfirmResponse;
import com.mkr.commerce.billing.dto.BillingConfirmRequest;
import com.mkr.commerce.billing.dto.CustomerBillSummaryDto;
import com.mkr.commerce.billing.dto.SerialCheckResponse;
import com.mkr.commerce.billing.entity.Bill;
import com.mkr.commerce.billing.entity.BillLineItem;
import com.mkr.commerce.billing.repository.BillLineItemRepository;
import com.mkr.commerce.billing.repository.BillRepository;
import com.mkr.commerce.catalog.entity.Product;
import com.mkr.commerce.catalog.repository.ProductRepository;
import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.customer.enums.AuthMethod;
import com.mkr.commerce.customer.enums.CustomerType;
import com.mkr.commerce.customer.repository.CustomerRepository;
import com.mkr.commerce.khata.entity.KhataEntry;
import com.mkr.commerce.khata.enums.KhataEntryType;
import com.mkr.commerce.khata.repository.KhataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository         billRepository;
    private final BillLineItemRepository lineItemRepository;
    private final ProductRepository      productRepository;
    private final CustomerRepository     customerRepository;
    private final KhataRepository        khataRepository;

    // ── Confirm ───────────────────────────────────────────────────────────────

    @Transactional
    public BillConfirmResponse confirm(BillingConfirmRequest req) {

        // 1. Resolve customer
        Customer customer = resolveCustomer(req);

        // 2. Load products by ID (single query)
        List<UUID> productIds = req.items().stream()
                .map(i -> i.productId())
                .toList();
        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 3. Validate existence and stock; compute line items
        List<BillLineItem> lineItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        for (var item : req.items()) {
            Product p = productMap.get(item.productId());
            if (p == null) {
                throw new ResourceNotFoundException("Product not found: " + item.productId());
            }
            if (p.getStockQty() < item.qty()) {
                throw new BadRequestException(
                    "Insufficient stock for '" + p.getName() + "'. Available: " + p.getStockQty()
                );
            }

            BigDecimal lineNet    = item.unitPrice()
                                        .multiply(BigDecimal.valueOf(item.qty()));
            BigDecimal gstPercent = req.gstEnabled() ? p.getGstPercent() : BigDecimal.ZERO;
            BigDecimal gstAmount  = lineNet
                                        .multiply(gstPercent)
                                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal  = lineNet.add(gstAmount);

            subtotal = subtotal.add(lineNet);
            totalGst = totalGst.add(gstAmount);

            // Deduct stock
            p.setStockQty(p.getStockQty() - item.qty());
            productRepository.save(p);

            List<String> serials = item.serialNumbers() != null
                    ? item.serialNumbers().stream().filter(s -> s != null && !s.isBlank()).toList()
                    : List.of();

            for (String sn : serials) {
                SerialCheckResponse check = checkSerial(sn);
                if (check.used()) {
                    throw new BadRequestException(
                        "Serial number '" + sn + "' was already sold in Bill #" + check.billNumber()
                    );
                }
            }

            lineItems.add(BillLineItem.builder()
                    .product(p)
                    .productName(p.getName())
                    .productSku(p.getSku())
                    .qty(item.qty())
                    .unitPrice(item.unitPrice())
                    .discount(BigDecimal.ZERO)
                    .gstPercent(gstPercent)
                    .lineTotal(lineTotal)
                    .gstAmount(gstAmount)
                    .serialNumbers(new ArrayList<>(serials))
                    .build());
        }

        BigDecimal grandTotal  = subtotal.add(totalGst);
        BigDecimal khataAmount = req.khataAmount();
        BigDecimal paidNow     = grandTotal.subtract(khataAmount);

        // 4. Build and save Bill
        Long billNum = billRepository.nextBillNumber();
        Bill bill = Bill.builder()
                .billNumber(billNum)
                .customer(customer)
                .customerPhone(customer != null ? safePhone(customer) : "")
                .customerName(customer != null ? customer.getName() : "Walk-in")
                .gstEnabled(req.gstEnabled())
                .paymentMethod(req.paymentMethod())
                .subtotal(subtotal)
                .totalDiscount(BigDecimal.ZERO)
                .gstAmount(totalGst)
                .grandTotal(grandTotal)
                .paidNow(paidNow)
                .khataAmount(khataAmount)
                .build();

        // Wire the bill reference into each line item
        for (BillLineItem li : lineItems) {
            li.setBill(bill);
        }
        bill.getLineItems().addAll(lineItems);

        Bill saved = billRepository.save(bill);

        // 5. Update customer stats and khata
        if (customer != null) {
            customer.setTotalOrders(customer.getTotalOrders() + 1);
            customer.setTotalSpent(customer.getTotalSpent().add(paidNow));
            customer.setLastOrderAt(Instant.now());

            if (khataAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal existing   = customer.getPendingAmount() != null ? customer.getPendingAmount() : BigDecimal.ZERO;
                BigDecimal newBalance = existing.add(khataAmount);
                customer.setPendingAmount(newBalance);

                KhataEntry entry = KhataEntry.builder()
                        .customer(customer)
                        .entryType(KhataEntryType.AUTO_ORDER)
                        .description("Bill " + saved.getBillId())
                        .debit(khataAmount)
                        .credit(BigDecimal.ZERO)
                        .balance(newBalance)
                        .orderId(saved.getBillId())
                        .paymentMethod(req.paidViaMethod())
                        .build();
                khataRepository.save(entry);
            }

            customerRepository.save(customer);
        }

        return BillConfirmResponse.from(saved);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public BillConfirmResponse getById(UUID id) {
        Bill bill = billRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
        return BillConfirmResponse.from(bill);
    }

    // ── Bills for a customer ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CustomerBillSummaryDto> getByCustomer(UUID customerId) {
        return billRepository.findByCustomerIdWithItemsOrderByCreatedAtDesc(customerId)
                .stream()
                .map(CustomerBillSummaryDto::from)
                .toList();
    }

    // ── List (paginated) ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<BillConfirmResponse> list(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String searchParam = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%" : null;

        Page<UUID> idsPage = billRepository.findIdsForList(searchParam, pageable);
        if (idsPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Map<UUID, Bill> billMap = billRepository.findByIdsWithItems(idsPage.getContent())
                .stream()
                .collect(Collectors.toMap(Bill::getId, Function.identity()));

        // Preserve the order returned by findIdsForList (createdAt DESC)
        List<BillConfirmResponse> responses = idsPage.getContent().stream()
                .map(billMap::get)
                .filter(Objects::nonNull)
                .map(BillConfirmResponse::from)
                .toList();

        return new PageImpl<>(responses, pageable, idsPage.getTotalElements());
    }

    // ── Serial number check ───────────────────────────────────────────────────

    public SerialCheckResponse checkSerial(String sn) {
        String cleaned = sn == null ? "" : sn.trim().toUpperCase();
        if (cleaned.isEmpty()) return new SerialCheckResponse(false, null);
        // Wrap in JSON quotes so the JSONB @> operator matches an exact array element
        String snJson = "\"" + cleaned + "\"";
        return lineItemRepository.findBillNumberBySerialNumber(snJson)
                .map(n -> new SerialCheckResponse(true, n))
                .orElse(new SerialCheckResponse(false, null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Customer resolveCustomer(BillingConfirmRequest req) {
        // Priority 1: UUID → existing known customer
        if (req.customerId() != null) {
            return customerRepository.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + req.customerId()));
        }

        // Priority 2: phone → search; create if not found
        if (req.customerPhone() != null && !req.customerPhone().isBlank()) {
            return customerRepository.findByPhone(req.customerPhone().trim())
                    .orElseGet(() -> createCustomer(req, AuthMethod.MOBILE));
        }

        // Priority 3: walk-in — create minimal record
        return createCustomer(req, AuthMethod.MOBILE);
    }

    private Customer createCustomer(BillingConfirmRequest req, AuthMethod authMethod) {
        String name  = req.customerName() != null && !req.customerName().isBlank()
                ? req.customerName().trim() : "Walk-in";
        String phone = req.customerPhone() != null && !req.customerPhone().isBlank()
                ? req.customerPhone().trim() : null;

        // Skip creation if truly anonymous (no name, no phone)
        if (phone == null && "Walk-in".equals(name)) {
            return null;
        }

        String[] parts     = name.split("\\s+", 2);
        String   firstName = parts[0];
        String   lastName  = parts.length > 1 ? parts[1] : parts[0];

        Long custNum = customerRepository.nextCustomerNumber();

        Customer c = Customer.builder()
                .customerNumber(custNum)
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .email(req.customerEmail() != null && !req.customerEmail().isBlank()
                        ? req.customerEmail().trim() : null)
                .authMethod(authMethod)
                .type(CustomerType.RETAIL)
                .isActive(true)
                .build();
        c.composeName();
        return customerRepository.save(c);
    }

    private String safePhone(Customer c) {
        return c.getPhone() != null ? c.getPhone() : "";
    }
}
