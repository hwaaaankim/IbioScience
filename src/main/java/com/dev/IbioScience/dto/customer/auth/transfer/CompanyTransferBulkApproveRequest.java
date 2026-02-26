package com.dev.IbioScience.dto.customer.auth.transfer;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyTransferBulkApproveRequest {
	private List<Long> applicationIds;
	private String processNote;
}