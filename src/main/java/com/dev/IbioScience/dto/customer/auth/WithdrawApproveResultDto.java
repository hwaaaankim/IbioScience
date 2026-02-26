package com.dev.IbioScience.dto.customer.auth;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawApproveResultDto {
	private int processedCount;
	private List<Long> failedIds;
}