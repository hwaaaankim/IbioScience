package com.dev.IbioScience.repository.auth.coupon;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.model.product.relation.MemberCoupon;

public interface AdminClientBenefitCouponRepository extends JpaRepository<MemberCoupon, Long> {

    @EntityGraph(attributePaths = {"coupon"})
    @Query("""
        select mc
          from MemberCoupon mc
         where mc.member.id = :memberId
           and (mc.deletedYn = false or mc.deletedYn is null)
           and (:fromAt is null or mc.issuedAt >= :fromAt)
           and (:toAt is null or mc.issuedAt < :toAt)
           and mc.status in :statuses
         order by mc.issuedAt desc, mc.id desc
    """)
    Page<MemberCoupon> searchActiveCoupons(
            @Param("memberId") Long memberId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            @Param("statuses") List<CouponStatus> statuses,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"coupon", "member"})
    @Query("""
        select mc
          from MemberCoupon mc
         where mc.id = :memberCouponId
           and mc.member.id = :memberId
           and (mc.deletedYn = false or mc.deletedYn is null)
    """)
    Optional<MemberCoupon> findActiveDetail(
            @Param("memberCouponId") Long memberCouponId,
            @Param("memberId") Long memberId
    );
}