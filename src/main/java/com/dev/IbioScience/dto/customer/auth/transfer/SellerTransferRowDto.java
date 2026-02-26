package com.dev.IbioScience.dto.customer.auth.transfer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SellerTransferRowDto {

	private Long applicationId;

	private String username;
	private String companyName; // applicant.companyProfile.companyName
	private String name;
	private String mobile;

	private String requestedAt;
}