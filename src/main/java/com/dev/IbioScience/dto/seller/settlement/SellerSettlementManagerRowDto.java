package com.dev.IbioScience.dto.seller.settlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.settlement.SettlementPayStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerSettlementManagerRowDto {

    private Long id;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private SettlementCycle settlementCycle;
    private SettlementBasis settlementBasis;
    private BigDecimal commissionRate;
    private Long grossAmount;
    private Long commissionAmount;
    private Long settlementAmount;
    private Integer orderCount;
    private Integer itemCount;
    private SettlementPayStatus payStatus;
    private LocalDateTime executedAt;
    private LocalDateTime paidAt;
}