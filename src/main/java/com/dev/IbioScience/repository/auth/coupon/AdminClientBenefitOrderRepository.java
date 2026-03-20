package com.dev.IbioScience.repository.auth.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.order.Order;

public interface AdminClientBenefitOrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findFirstByMemberCouponIdOrderByCreatedAtDesc(Long memberCouponId);
}