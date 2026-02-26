package com.dev.IbioScience.dto.customer.auth.transfer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyTransferSearchRequest {

	private Integer page;     // 0-base
	private Integer size;     // 10/30/50/100

	private String fromDate;  // yyyy-MM-dd (optional)
	private String toDate;    // yyyy-MM-dd (optional)

	/**
	 * USERNAME / MOBILE / NAME
	 */
	private String searchType;
	private String keyword;

	/**
	 * 정렬 키: username/companyName/name/mobile/requestedAt
	 */
	private String sortKey;
	/**
	 * asc/desc
	 */
	private String sortDir;
}