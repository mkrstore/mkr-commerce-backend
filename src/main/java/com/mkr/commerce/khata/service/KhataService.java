package com.mkr.commerce.khata.service;

import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.customer.repository.CustomerRepository;
import com.mkr.commerce.khata.dto.AddKhataEntryRequest;
import com.mkr.commerce.khata.dto.CollectPaymentRequest;
import com.mkr.commerce.khata.dto.KhataEntryDto;
import com.mkr.commerce.khata.dto.KhataPageDto;
import com.mkr.commerce.khata.entity.KhataEntry;
import com.mkr.commerce.khata.enums.KhataEntryType;
import com.mkr.commerce.khata.repository.KhataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KhataService {

    private final KhataRepository    khataRepository;
    private final CustomerRepository customerRepository;

    // ── Get all entries ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public KhataPageDto getEntries(UUID customerId) {
        Customer customer = findCustomerOrThrow(customerId);

        List<KhataEntryDto> entries = khataRepository
                .findByCustomerIdOrderByCreatedAtAsc(customerId)
                .stream()
                .map(KhataEntryDto::from)
                .toList();

        return new KhataPageDto(entries, customer.getPendingAmount());
    }

    // ── Add manual entry (debit or credit) ────────────────────────────────────

    @Transactional
    public KhataEntryDto addEntry(UUID customerId, AddKhataEntryRequest req) {
        if (req.entryType() == KhataEntryType.AUTO_ORDER) {
            throw new BadRequestException("AUTO_ORDER entries are created by the order system only.");
        }

        Customer   customer    = findCustomerOrThrow(customerId);
        BigDecimal prevBalance = customer.getPendingAmount();
        BigDecimal amount      = req.amount();

        BigDecimal debit, credit, newBalance;

        if (req.entryType() == KhataEntryType.MANUAL_DEBIT) {
            debit      = amount;
            credit     = BigDecimal.ZERO;
            newBalance = prevBalance.add(amount);
        } else {
            debit      = BigDecimal.ZERO;
            credit     = amount;
            newBalance = prevBalance.subtract(amount).max(BigDecimal.ZERO);
        }

        KhataEntry entry = KhataEntry.builder()
                .customer(customer)
                .entryType(req.entryType())
                .description(req.description())
                .debit(debit)
                .credit(credit)
                .balance(newBalance)
                .notes(req.notes())
                .build();

        customer.setPendingAmount(newBalance);
        customerRepository.save(customer);
        KhataEntry saved = khataRepository.save(entry);

        log.info("Khata entry added: {} [{}] {} {} → balance {}",
                customer.getName(), customer.getCustomerId(),
                req.entryType(), amount, newBalance);

        return KhataEntryDto.from(saved);
    }

    // ── Collect payment ───────────────────────────────────────────────────────

    @Transactional
    public KhataEntryDto collectPayment(UUID customerId, CollectPaymentRequest req) {
        Customer   customer    = findCustomerOrThrow(customerId);
        BigDecimal prevBalance = customer.getPendingAmount();

        if (prevBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This customer has no outstanding balance to collect.");
        }

        BigDecimal amount     = req.amount().min(prevBalance);
        BigDecimal newBalance = prevBalance.subtract(amount).max(BigDecimal.ZERO);

        String description = "Payment collected via " + req.paymentMethod()
                + (req.note() != null && !req.note().isBlank() ? " · " + req.note().trim() : "");

        KhataEntry entry = KhataEntry.builder()
                .customer(customer)
                .entryType(KhataEntryType.MANUAL_CREDIT)
                .description(description)
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .balance(newBalance)
                .paymentMethod(req.paymentMethod())
                .notes(req.note())
                .build();

        customer.setPendingAmount(newBalance);
        customerRepository.save(customer);
        KhataEntry saved = khataRepository.save(entry);

        log.info("Payment collected: {} [{}] {} via {} → balance {}",
                customer.getName(), customer.getCustomerId(),
                amount, req.paymentMethod(), newBalance);

        return KhataEntryDto.from(saved);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Customer findCustomerOrThrow(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
    }
}
