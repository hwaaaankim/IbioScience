package com.dev.IbioScience.dto.seller.settlement;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerSettlementManagerPageResponse {

    private List<SellerSettlementManagerRowDto> content;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasPrevious;
    private boolean hasNext;
}