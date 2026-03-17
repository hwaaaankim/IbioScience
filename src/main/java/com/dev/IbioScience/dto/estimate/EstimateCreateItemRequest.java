package com.dev.IbioScience.dto.estimate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimateCreateItemRequest {

    private Long mappingId;
    private Integer quantity;
}