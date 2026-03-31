package com.dev.IbioScience.dto.settlement;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementExecuteResultResponse {
    private Long batchId;
    private Integer createdCount;
    private List<SettlementExecutePreviewRowDto> createdItems;
}