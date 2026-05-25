package com.mkr.commerce.order.service;

import com.mkr.commerce.common.exception.ResourceNotFoundException;
import com.mkr.commerce.customer.repository.CustomerRepository;
import com.mkr.commerce.order.dto.CustomerOrderDto;
import com.mkr.commerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository    orderRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerOrderDto> getByCustomer(UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer not found: " + customerId);
        }
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(CustomerOrderDto::from)
                .toList();
    }
}
