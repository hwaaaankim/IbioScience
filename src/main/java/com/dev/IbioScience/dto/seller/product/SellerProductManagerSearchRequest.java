package com.dev.IbioScience.dto.seller.product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;

import com.dev.IbioScience.enums.product.DisplayStatus;
import com.dev.IbioScience.enums.product.SaleStatus;
import com.dev.IbioScience.enums.product.dealer.SellerProductManagerDateType;
import com.dev.IbioScience.enums.product.dealer.SellerProductManagerSearchType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerProductManagerSearchRequest {

    private SellerProductManagerDateType dateType = SellerProductManagerDateType.CREATED_AT;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private List<DisplayStatus> displayStatuses = new ArrayList<>();
    private List<SaleStatus> saleStatuses = new ArrayList<>();

    private SellerProductManagerSearchType searchType = SellerProductManagerSearchType.PRODUCT_NAME;
    private String keyword;

    private Long largeId;
    private Long mediumId;
    private Long smallId;

    private Integer page = 0;
    private Integer size = 10;

    public int getValidatedPage() {
        return (page == null || page < 0) ? 0 : page;
    }

    public int getValidatedSize() {
        Set<Integer> allowed = Set.of(10, 30, 50, 100);
        return (size != null && allowed.contains(size)) ? size : 10;
    }

    public String getTrimmedKeyword() {
        return keyword == null ? "" : keyword.trim();
    }
}