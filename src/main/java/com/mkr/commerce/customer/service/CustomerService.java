package com.mkr.commerce.customer.service;

import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.customer.dto.CustomerDetailDto;
import com.mkr.commerce.customer.dto.CustomerSummaryDto;
import com.mkr.commerce.customer.entity.Customer;
import com.mkr.commerce.customer.enums.AuthMethod;
import com.mkr.commerce.customer.enums.CustomerType;
import com.mkr.commerce.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    // ── List ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<CustomerSummaryDto> list(
            CustomerType type,
            boolean pendingOnly,
            String search,
            Pageable pageable
    ) {
        String  searchParam   = null;
        Long    customerNum   = null;
        Boolean pendingFilter = pendingOnly ? Boolean.TRUE : null;

        if (search != null && !search.isBlank()) {
            String s = search.trim().toLowerCase();
            if (s.matches("cus-?\\d+")) {
                customerNum = Long.parseLong(s.replaceAll("[^\\d]", ""));
            } else {
                searchParam = "%" + s + "%";
            }
        }

        return customerRepository
                .findCustomers(type, pendingFilter, searchParam, customerNum, pageable)
                .map(CustomerSummaryDto::from);
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerDetailDto getById(UUID id) {
        return CustomerDetailDto.from(findOrThrow(id));
    }

    // ── Update type ───────────────────────────────────────────────────────────

    @Transactional
    public CustomerDetailDto updateType(UUID id, CustomerType type) {
        Customer customer = findOrThrow(id);
        CustomerType old  = customer.getType();
        customer.setType(type);
        Customer saved = customerRepository.save(customer);
        log.info("Customer type updated: {} [{}] {} → {}", saved.getName(), saved.getCustomerId(), old, type);
        return CustomerDetailDto.from(saved);
    }

    // ── Update status ─────────────────────────────────────────────────────────

    @Transactional
    public CustomerDetailDto updateStatus(UUID id, boolean active) {
        Customer customer = findOrThrow(id);
        customer.setActive(active);
        Customer saved = customerRepository.save(customer);
        log.info("Customer status updated: {} [{}] → {}", saved.getName(), saved.getCustomerId(),
                active ? "ACTIVE" : "INACTIVE");
        return CustomerDetailDto.from(saved);
    }

    // ── Register (called by auth module when a new customer registers) ────────

    @Transactional
    public Customer register(
            String firstName,
            String lastName,
            String email,
            String phone,
            String passwordHash,
            String googleId,
            AuthMethod authMethod
    ) {
        Long num = customerRepository.nextCustomerNumber();

        Customer customer = Customer.builder()
                .customerNumber(num)
                .firstName(firstName)
                .lastName(lastName)
                .email(email != null ? email.toLowerCase().strip() : null)
                .phone(phone)
                .passwordHash(passwordHash)
                .googleId(googleId)
                .authMethod(authMethod)
                .type(CustomerType.RETAIL)
                .isActive(true)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .build();
        customer.composeName();

        Customer saved = customerRepository.save(customer);
        log.info("New customer registered: {} [{}] via {}", saved.getName(), saved.getCustomerId(), authMethod);
        return saved;
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Customer findOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
