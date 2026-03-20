package com.dev.IbioScience.service.product.front;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.front.paymentStart.CustomerCouponResponse;
import com.dev.IbioScience.dto.front.paymentStart.CustomerMemberMeResponse;
import com.dev.IbioScience.enums.product.CouponStatus;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.relation.MemberCoupon;
import com.dev.IbioScience.repository.auth.MemberCouponRepository;
import com.dev.IbioScience.repository.auth.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerPaymentQueryService {

    private final MemberRepository memberRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Transactional(readOnly = true)
    public CustomerMemberMeResponse getMyMemberProfile(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. memberId=" + memberId));

        return CustomerMemberMeResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .mobile(m.getMobile())
                .tel(m.getTel())
                .point(m.getPoint() == null ? 0L : m.getPoint())
                .address(m.getAddress())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CustomerCouponResponse> getMyUsableCoupons(Long memberId, LocalDate issuedStart, LocalDate issuedEnd) {

        LocalDate s = (issuedStart != null) ? issuedStart : LocalDate.of(1, 1, 1);
        LocalDate e = (issuedEnd != null) ? issuedEnd : LocalDate.of(9999, 12, 31);

        LocalDateTime startDt = LocalDateTime.of(s, LocalTime.MIN);
        LocalDateTime endDt = LocalDateTime.of(e, LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        // issuedAt 기준 조회
        List<MemberCoupon> list1 = memberCouponRepository.findSearchableIssuedByMemberAndIssuedAtBetween(
                memberId,
                CouponStatus.ISSUED,
                startDt,
                endDt,
                now
        );

        // issuedAt 이 null 인 데이터는 createdAt 기준 fallback 조회
        List<MemberCoupon> list2 = memberCouponRepository.findSearchableIssuedByMemberAndCreatedAtBetweenWhenIssuedAtNull(
                memberId,
                CouponStatus.ISSUED,
                startDt,
                endDt,
                now
        );

        List<MemberCoupon> all = new ArrayList<>();
        all.addAll(list1);
        all.addAll(list2);

        // 방어적으로 중복 제거 (기존 기능은 유지하면서 dirty data 대비)
        Map<Long, CustomerCouponResponse> resultMap = new LinkedHashMap<>();

        for (MemberCoupon mc : all) {
            if (mc == null || mc.getId() == null) {
                continue;
            }

            Coupon c = mc.getCoupon();
            if (c == null) {
                continue;
            }

            // 쿠폰 자체 기간도 여전히 체크
            if (c.getStartDate() != null && today.isBefore(c.getStartDate())) {
                continue;
            }
            if (c.getEndDate() != null && today.isAfter(c.getEndDate())) {
                continue;
            }

            String issuedDate;
            if (mc.getIssuedAt() != null) {
                issuedDate = mc.getIssuedAt().toLocalDate().toString();
            } else if (mc.getCreatedAt() != null) {
                issuedDate = mc.getCreatedAt().toLocalDate().toString();
            } else {
                issuedDate = "";
            }

            resultMap.putIfAbsent(
                    mc.getId(),
                    CustomerCouponResponse.builder()
                            .memberCouponId(mc.getId())
                            .couponId(c.getId())
                            .couponCode(c.getCouponCode())
                            .couponName(c.getCouponName())
                            .minPurchaseAmount(c.getMinPurchaseAmount())
                            .couponAmount(c.getCouponAmount())
                            .startDate(c.getStartDate() != null ? c.getStartDate().toString() : "")
                            .endDate(c.getEndDate() != null ? c.getEndDate().toString() : "")
                            .issuedDate(issuedDate)
                            .build()
            );
        }

        return new ArrayList<>(resultMap.values());
    }
}