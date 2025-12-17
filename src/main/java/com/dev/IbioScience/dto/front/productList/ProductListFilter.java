package com.dev.IbioScience.dto.front.productList;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class ProductListFilter {

    // 날짜 기준: createdAt / updatedAt
    private String dateField; // createdAt | updatedAt

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateTo;

    // 진열/판매 상태
    private String displayStatus; // ON | OFF | ""
    private String saleStatus;    // ON | OFF | ""

    // 검색
    private String searchType; // name | internalProductCode | code | brand
    private String keyword;

    // 분류
    private String categoryMode; // INTERNAL | EXTERNAL | ""
    private Long largeId;
    private Long mediumId;
    private Long smallId;

    // 페이징
    private Integer page; // 1-base
    private Integer size; // 10/30/50/100
}