package com.dev.IbioScience.dto.settlement;

import java.time.LocalDate;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementExecutePreviewRowDto {
    private Long sellerDealerProfileId;
    private String memberUsername;
    private String memberName;
    private String companyName;
    private String shopName;
    private SettlementCycle cycle;
    private SettlementBasis basis;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private Integer orderCount;
    private Integer itemCount;
    private Long grossAmount;
    private Long commissionAmount;
    private Long settlementAmount;
}