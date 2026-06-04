package com.mkr.commerce.customer.service;

import com.mkr.commerce.common.exception.BadRequestException;
import com.mkr.commerce.common.exception.ErrorCode;
import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.customer.dto.CreateCustomerRequest;
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

    // ── Update full details ───────────────────────────────────────────────────

    @Transactional
    public CustomerDetailDto update(UUID id, com.mkr.commerce.customer.dto.UpdateCustomerRequest req) {
        Customer customer = findOrThrow(id);

        customer.setFirstName(req.firstName().trim());
        customer.setLastName(req.lastName().trim());
        customer.composeName();

        if (req.phone() != null && !req.phone().isBlank()) {
            // Only update phone if changed — phone is unique, skip if same
            if (!req.phone().equals(customer.getPhone())) {
                customer.setPhone(req.phone().trim());
            }
        }

        if (req.email() != null && !req.email().isBlank()) {
            customer.setEmail(req.email().toLowerCase().trim());
        } else {
            customer.setEmail(null);
        }

        if (req.type() != null) customer.setType(req.type());

        customer.setAddressStreet(blank(req.addressStreet()));
        customer.setAddressCity(blank(req.addressCity()));
        customer.setAddressMandal(blank(req.addressMandal()));
        customer.setAddressDistrict(blank(req.addressDistrict()));
        customer.setAddressState(blank(req.addressState()));
        customer.setAddressPostalCode(blank(req.addressPostalCode()));

        Customer saved = customerRepository.save(customer);
        log.info("Customer updated: {} [{}]", saved.getName(), saved.getCustomerId());
        return CustomerDetailDto.from(saved);
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

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

    // ── Create (called by staff from admin portal) ────────────────────────────

    @Transactional
    public CustomerDetailDto create(CreateCustomerRequest req) {
        if (customerRepository.existsByPhone(req.phone())) {
            throw new BadRequestException("Phone number is already registered to another customer", ErrorCode.DUPLICATE_PHONE);
        }

        String email = (req.email() != null && !req.email().isBlank())
                ? req.email().toLowerCase().strip()
                : null;

        if (email != null && customerRepository.existsByEmail(email)) {
            throw new BadRequestException("Email address is already registered to another customer", ErrorCode.DUPLICATE_EMAIL);
        }

        Long num = customerRepository.nextCustomerNumber();

        Customer customer = Customer.builder()
                .customerNumber(num)
                .firstName(req.firstName().trim())
                .lastName(req.lastName().trim())
                .email(email)
                .phone(req.phone().trim())
                .authMethod(AuthMethod.USER_ID)
                .type(req.type() != null ? req.type() : CustomerType.RETAIL)
                .isActive(true)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .pendingAmount(BigDecimal.ZERO)
                .addressStreet(blank(req.addressStreet()))
                .addressCity(blank(req.addressCity()))
                .addressMandal(blank(req.addressMandal()))
                .addressDistrict(blank(req.addressDistrict()))
                .addressState(blank(req.addressState()))
                .addressPostalCode(blank(req.addressPostalCode()))
                .build();
        customer.composeName();

        Customer saved = customerRepository.save(customer);
        log.info("New customer created by staff: {} [{}]", saved.getName(), saved.getCustomerId());
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
