package com.dev.IbioScience.dto.settlement;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SettlementManagerPageResponse {
    private List<SettlementManagerRowDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}