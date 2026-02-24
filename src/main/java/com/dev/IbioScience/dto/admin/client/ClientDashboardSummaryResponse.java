package com.dev.IbioScience.dto.admin.client;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientDashboardSummaryResponse {

	private String date; // yyyy-MM-dd
	private long visitPv;
	private long visitUv;

	@Builder.Default
	private List<Item> items = new ArrayList<>();

	@Getter
	@Builder
	public static class Item {
		private String key;       // action key or metric key
		private String label;     // card title
		private long count;       // number
		private boolean drilldown; // true => clickable logs table
	}
}