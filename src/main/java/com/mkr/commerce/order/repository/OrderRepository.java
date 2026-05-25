package com.mkr.commerce.order.repository;

import com.mkr.commerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @Query(value = "SELECT nextval('order_num_seq')", nativeQuery = true)
    Long nextOrderNumber();
}
