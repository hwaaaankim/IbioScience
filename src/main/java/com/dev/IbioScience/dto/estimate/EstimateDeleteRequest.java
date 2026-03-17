package com.dev.IbioScience.dto.estimate;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstimateDeleteRequest {

    private List<Long> estimateIds;
}