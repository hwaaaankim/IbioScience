package com.dev.IbioScience.dto.settlement;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.enums.settlement.SettlementPayStatus;

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
public class SettlementManagerRowDto {

    private Long id;

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

    private LocalDateTime executedAt;
    private LocalDateTime paidAt;

    private SettlementPayStatus payStatus;
}