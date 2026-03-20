package com.dev.IbioScience.dto.admin.wishList;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminClientWishListSearchCondition {

    private int page;
    private int size;

    private LocalDate fromDate;
    private LocalDate toDate;

    private Long largeId;
    private Long mediumId;
    private Long smallId;

    private String productName;
}