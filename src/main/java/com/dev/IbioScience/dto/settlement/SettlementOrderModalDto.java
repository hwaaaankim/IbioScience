package com.dev.IbioScience.dto.settlement;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementOrderModalDto {
    private Long orderId;
    private String orderNo;
    private String ordererName;
    private LocalDateTime basisDate;
    private Integer dealerItemCount;
    private Long dealerItemAmount;
}