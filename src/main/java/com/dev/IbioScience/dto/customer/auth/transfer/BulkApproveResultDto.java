package com.dev.IbioScience.dto.customer.auth.transfer;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BulkApproveResultDto {

	private int requestedCount;
	private int successCount;
	private int failCount;

	@Builder.Default
	private List<FailureItem> failures = new ArrayList<>();

	@Getter
	@Setter
	@Builder
	public static class FailureItem {
		private Long applicationId;
		private String reason;
	}
}