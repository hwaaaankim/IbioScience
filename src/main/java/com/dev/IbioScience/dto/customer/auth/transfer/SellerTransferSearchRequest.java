package com.dev.IbioScience.dto.customer.auth.transfer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerTransferSearchRequest {

	private Integer page;
	private Integer size;

	private String fromDate; // yyyy-MM-dd
	private String toDate;   // yyyy-MM-dd

	/**
	 * USERNAME / MOBILE / NAME
	 */
	private String searchType;
	private String keyword;

	/**
	 * 정렬 키: username/companyName/name/mobile/requestedAt
	 */
	private String sortKey;
	private String sortDir;
}