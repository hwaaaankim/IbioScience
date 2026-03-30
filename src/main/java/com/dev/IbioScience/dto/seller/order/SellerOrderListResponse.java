package com.dev.IbioScience.dto.seller.order;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SellerOrderListResponse {

    private List<SellerOrderListItemDto> content;

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    private boolean first;
    private boolean last;
    private boolean empty;

    private String sortField;
    private String sortDir;
}