package com.dev.IbioScience.dto.settlement;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettlementOrderSummarySourceDto {
    private Long orderId;
    private String orderNo;
    private LocalDateTime basisDate;
    private Long dealerAmount;
    private Long dealerItemCount;
    private String ordererName;
}