package com.dev.IbioScience.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.order.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}