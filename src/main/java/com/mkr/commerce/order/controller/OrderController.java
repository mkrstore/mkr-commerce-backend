package com.mkr.commerce.order.controller;

import com.mkr.commerce.common.response.ApiResponse;
import com.mkr.commerce.order.dto.CustomerOrderDto;
import com.mkr.commerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers/{customerId}/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerOrderDto>>> getByCustomer(
            @PathVariable UUID customerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Orders", orderService.getByCustomer(customerId)));
    }
}
