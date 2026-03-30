package com.dev.IbioScience.repository.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.model.order.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
	
	@Query("""
        select distinct oi
        from OrderItem oi
        join oi.dealerProduct dp
        join dp.sellerDealerProfile sdp
        join sdp.member sellerMember
        where oi.order.id = :orderId
          and oi.itemProductType = 'DEALER'
          and sellerMember.id = :sellerMemberId
        order by oi.id asc
    """)
    List<OrderItem> findSellerVisibleDealerItems(@Param("orderId") Long orderId,
                                                 @Param("sellerMemberId") Long sellerMemberId);
}