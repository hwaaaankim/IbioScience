package com.dev.IbioScience.dto.customer.auth.transfer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CompanyTransferRowDto {

	private Long applicationId;

	private String username;
	private String companyName;
	private String name;
	private String mobile;

	private String requestedAt; // yyyy-MM-dd HH:mm
}