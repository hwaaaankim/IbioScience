package com.dev.IbioScience.repository.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.order.PaymentMethod;
import com.dev.IbioScience.model.order.Order;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order>  {
    Optional<Order> findByOrderNo(String orderNo);
    Optional<Order> findByMember_IdAndOrderNo(Long memberId, String orderNo);
    boolean existsByOrderNo(String orderNo);
    

    /**
     * "계좌이체 + paidAt == null" 기준으로 조회합니다.
     */
    List<Order> findByPaymentMethodAndPaidAtIsNullOrderByCreatedAtAsc(PaymentMethod paymentMethod);
    
    @Override
    @EntityGraph(attributePaths = {
            "member",
            "member.companyProfile",
            "member.sellerDealerProfile"
    })
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "member",
            "member.companyProfile",
            "member.buyerDealerProfile",
            "member.sellerDealerProfile",
            "items",
            "items.product",
            "items.productOptionGroup",
            "items.productOption"
    })
    @Query("""
            select o
            from Order o
            where o.id = :orderId
            """)
    Optional<Order> findDetailById(@Param("orderId") Long orderId);
}