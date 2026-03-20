package com.dev.IbioScience.repository.auth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.model.product.relation.MemberCoupon;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByMember_IdAndCoupon_Id(Long memberId, Long couponId);

    /**
     * 주문 적용용:
     * - 본인 쿠폰
     * - ISSUED 상태
     * - 소프트삭제 아님
     * - 아직 사용 안 함
     * - 멤버쿠폰 expiredAt 기준 만료 아님
     */
    @Query("""
        select mc
        from MemberCoupon mc
        join fetch mc.coupon c
        where mc.id = :memberCouponId
          and mc.member.id = :memberId
          and mc.status = :status
          and coalesce(mc.deletedYn, false) = false
          and mc.usedAt is null
          and (mc.expiredAt is null or mc.expiredAt >= :now)
    """)
    Optional<MemberCoupon> findUsableMemberCouponForOrder(
            @Param("memberId") Long memberId,
            @Param("memberCouponId") Long memberCouponId,
            @Param("status") CouponStatus status,
            @Param("now") LocalDateTime now
    );

    /**
     * 고객 쿠폰 조회용:
     * - ISSUED 상태
     * - 소프트삭제 아님
     * - 아직 사용 안 함
     * - issuedAt 존재
     * - issuedAt 기간 조건
     * - 멤버쿠폰 expiredAt 기준 만료 아님
     */
    @Query("""
        select mc
        from MemberCoupon mc
        join fetch mc.coupon c
        where mc.member.id = :memberId
          and mc.status = :status
          and coalesce(mc.deletedYn, false) = false
          and mc.usedAt is null
          and mc.issuedAt is not null
          and mc.issuedAt >= :start
          and mc.issuedAt <= :end
          and (mc.expiredAt is null or mc.expiredAt >= :now)
        order by mc.issuedAt desc, mc.id desc
    """)
    List<MemberCoupon> findSearchableIssuedByMemberAndIssuedAtBetween(
            @Param("memberId") Long memberId,
            @Param("status") CouponStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("now") LocalDateTime now
    );

    /**
     * issuedAt 이 null 인 예전 데이터 fallback 조회용:
     * - createdAt 기간 조건 사용
     * - 나머지 조건은 동일
     */
    @Query("""
        select mc
        from MemberCoupon mc
        join fetch mc.coupon c
        where mc.member.id = :memberId
          and mc.status = :status
          and coalesce(mc.deletedYn, false) = false
          and mc.usedAt is null
          and mc.issuedAt is null
          and mc.createdAt >= :start
          and mc.createdAt <= :end
          and (mc.expiredAt is null or mc.expiredAt >= :now)
        order by mc.createdAt desc, mc.id desc
    """)
    List<MemberCoupon> findSearchableIssuedByMemberAndCreatedAtBetweenWhenIssuedAtNull(
            @Param("memberId") Long memberId,
            @Param("status") CouponStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("now") LocalDateTime now
    );
}