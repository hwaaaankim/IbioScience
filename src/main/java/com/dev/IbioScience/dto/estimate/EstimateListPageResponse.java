package com.dev.IbioScience.dto.estimate;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstimateListPageResponse {

    private List<EstimateListItemDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private String sortBy;
    private String sortDir;
}