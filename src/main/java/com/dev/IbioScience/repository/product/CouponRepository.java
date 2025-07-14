package com.dev.IbioScience.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.product.Coupon;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    boolean existsByCouponCode(String couponCode);
}