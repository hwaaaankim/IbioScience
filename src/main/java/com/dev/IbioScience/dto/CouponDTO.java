package com.dev.IbioScience.dto;

import lombok.Data;

// 쿠폰 검색 위한 DTO
@Data
public class CouponDTO {
    private Long id;
    private String couponName;
    private String startDate;
    private String endDate;
    private String status;
}