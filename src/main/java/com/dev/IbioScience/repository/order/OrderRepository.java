package com.dev.IbioScience.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.model.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);
    Optional<Order> findByMember_IdAndOrderNo(Long memberId, String orderNo);
    boolean existsByOrderNo(String orderNo);
    

    /**
     * "계좌이체 + paidAt == null" 기준으로 조회합니다.
     */
    List<Order> findByPaymentMethodAndPaidAtIsNullOrderByCreatedAtAsc(PaymentMethod paymentMethod);
}