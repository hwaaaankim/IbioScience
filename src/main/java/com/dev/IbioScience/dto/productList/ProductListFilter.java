package com.dev.IbioScience.dto.productList;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProductListFilter {
    // 날짜 기준: createdAt / updatedAt
    private String dateField;               // "createdAt" | "updatedAt"
    private String dateQuick;               // "TODAY" | "D7" | "M1" | "RANGE"
    private LocalDate dateFrom;             // RANGE 일 때 사용
    private LocalDate dateTo;

    // 진열/판매 상태
    private String displayStatus;           // "ON" | "OFF" | ""(전체)
    private String saleStatus;              // "ON" | "OFF" | ""(전체)

    // 검색 타입: name/internalProductCode/code/brand
    private String searchType;              // "name" | "internalProductCode" | "code" | "brand"
    private String keyword;

    // 분류 필터
    private String categoryMode;            // "INTERNAL" | "EXTERNAL" | ""(전체)
    private Long largeId;
    private Long mediumId;
    private Long smallId;

    // 페이징
    private Integer page;                   // 1-base
    private Integer size;                   // 10/30/50/100
}