package com.dev.IbioScience.dto.page.productList;

public enum ProductSortOption {
    CREATED_AT_DESC,  // 등록일 최신순 (기본)
    NAME_ASC,         // 이름순
    NAME_DESC,        // 이름 역순
    PRICE_ASC,        // 가격 낮은순
    PRICE_DESC,       // 가격 높은순
    RATING_DESC,      // 별점 높은순
    RATING_ASC        // 별점 낮은순
}