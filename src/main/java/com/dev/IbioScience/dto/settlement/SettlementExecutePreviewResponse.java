package com.dev.IbioScience.dto.settlement;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementExecutePreviewResponse {
    private List<SettlementExecutePreviewRowDto> items;
    private Integer count;
    private Long totalGrossAmount;
    private Long totalCommissionAmount;
    private Long totalSettlementAmount;
}