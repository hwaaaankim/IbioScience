package com.dev.IbioScience.dto.estimate;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstimateCreateResponse {

    private Long estimateId;
    private String message;
}