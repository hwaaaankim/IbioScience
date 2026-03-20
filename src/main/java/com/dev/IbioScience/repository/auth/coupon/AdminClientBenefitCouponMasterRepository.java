package com.dev.IbioScience.repository.auth.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Coupon;

public interface AdminClientBenefitCouponMasterRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCouponCode(String couponCode);
}