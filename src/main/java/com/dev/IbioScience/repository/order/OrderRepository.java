package com.dev.IbioScience.repository.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);
    Optional<Order> findByMember_IdAndOrderNo(Long memberId, String orderNo);
    boolean existsByOrderNo(String orderNo);
}