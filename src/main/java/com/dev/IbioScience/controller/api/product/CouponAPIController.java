package com.dev.IbioScience.controller.api.product;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.CouponDTO;
import com.dev.IbioScience.service.product.CouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponAPIController {

    private final CouponService couponService;

    @GetMapping("/search")
    public List<CouponDTO> searchCoupons(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        return couponService.searchCoupons(status, name, startDate, endDate);
    }
}