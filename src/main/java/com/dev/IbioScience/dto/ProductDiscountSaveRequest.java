package com.dev.IbioScience.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

/* 할인혜택 저장용 */ 
@Data
public class ProductDiscountSaveRequest {
    private Boolean active;
    private String type; // DiscountType
    private String term; // DiscountTerm
    private String name;
    private Boolean periodEnabled;
    private String startDate; // yyyy-MM-dd
    private String endDate;   // yyyy-MM-dd
    private Boolean applyToAll;
    private Boolean applyToDealer;
    private Boolean applyToRegular;
    private String discountPercent;
    private String couponPolicy;
    private MultipartFile iconFile;
}