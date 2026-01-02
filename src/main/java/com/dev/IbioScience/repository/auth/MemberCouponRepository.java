package com.dev.IbioScience.repository.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.model.product.relation.MemberCoupon;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

	@Query("""
		select mc
		from MemberCoupon mc
		join fetch mc.coupon c
		where mc.member.id = :memberId
		  and mc.status = :status
		  and mc.issuedAt >= :start
		  and mc.issuedAt <= :end
		order by mc.issuedAt desc, mc.id desc
	""")
	List<MemberCoupon> findIssuedByMemberAndIssuedAtBetween(
			@Param("memberId") Long memberId,
			@Param("status") CouponStatus status,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);

	// ✅ issuedAt이 null인 데이터가 있을 수 있으니 createdAt 기준 버전도 같이 제공(안전)
	@Query("""
		select mc
		from MemberCoupon mc
		join fetch mc.coupon c
		where mc.member.id = :memberId
		  and mc.status = :status
		  and (mc.issuedAt is null)
		  and mc.createdAt >= :start
		  and mc.createdAt <= :end
		order by mc.createdAt desc, mc.id desc
	""")
	List<MemberCoupon> findIssuedByMemberAndCreatedAtBetweenWhenIssuedAtNull(
			@Param("memberId") Long memberId,
			@Param("status") CouponStatus status,
			@Param("start") LocalDateTime start,
			@Param("end") LocalDateTime end
	);
}