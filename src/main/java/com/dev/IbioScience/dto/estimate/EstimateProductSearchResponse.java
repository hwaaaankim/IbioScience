package com.dev.IbioScience.dto.estimate;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstimateProductSearchResponse {

    private List<EstimateProductRowDto> items;
}