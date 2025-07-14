package com.dev.IbioScience.service.product;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.CouponRegisterRequestDTO;
import com.dev.IbioScience.model.product.Coupon;
import com.dev.IbioScience.model.product.enums.CouponPolicy;
import com.dev.IbioScience.model.product.enums.CouponStatus;
import com.dev.IbioScience.repository.product.CouponRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    // 쿠폰 등록
    @Transactional
    public Coupon registerCoupon(CouponRegisterRequestDTO dto) {
        Coupon coupon = new Coupon();

        // 고유 쿠폰코드 생성(랜덤 UUID, 20자 이내 자르기)
        String generatedCode = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        coupon.setCouponCode(generatedCode);

        coupon.setCouponName(dto.getCouponName());
        coupon.setMinPurchaseAmount(dto.getMinPurchaseAmount());
        coupon.setCouponAmount(dto.getCouponAmount());
        coupon.setStartDate(dto.getStartDate());
        coupon.setEndDate(dto.getEndDate());

        // enum 문자열 -> Enum 변환
        if (dto.getCouponPolicy() != null) {
            coupon.setCouponPolicy(CouponPolicy.valueOf(dto.getCouponPolicy()));
        }

        coupon.setStatus(CouponStatus.ISSUED); // 기본값 발급됨

        return couponRepository.save(coupon);
    }
}