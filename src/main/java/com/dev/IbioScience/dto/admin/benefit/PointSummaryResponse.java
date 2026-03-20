package com.dev.IbioScience.dto.admin.benefit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PointSummaryResponse {

    private Long currentPoint;
}