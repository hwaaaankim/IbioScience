package com.dev.IbioScience.repository.auth.coupon;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.product.coupon.MemberCouponHistoryActionType;
import com.dev.IbioScience.model.product.coupon.MemberCouponHistory;

public interface AdminClientBenefitCouponHistoryRepository extends JpaRepository<MemberCouponHistory, Long> {

    List<MemberCouponHistory> findByMemberCouponIdInOrderByCreatedAtDesc(List<Long> memberCouponIds);

    List<MemberCouponHistory> findByMemberCouponIdOrderByCreatedAtDesc(Long memberCouponId);

    boolean existsByMemberCouponIdAndActionTypeAndOrderId(
            Long memberCouponId,
            MemberCouponHistoryActionType actionType,
            Long orderId
    );
}