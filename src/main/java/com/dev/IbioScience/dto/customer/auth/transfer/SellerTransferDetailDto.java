package com.dev.IbioScience.dto.customer.auth.transfer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SellerTransferDetailDto {

	private Long applicationId;

	private Long memberId;
	private String username;
	private String name;
	private String mobile;
	private String email;

	private String companyName;
	private String businessRegistrationNumber;

	private String requestedAt;
	private String note;
}