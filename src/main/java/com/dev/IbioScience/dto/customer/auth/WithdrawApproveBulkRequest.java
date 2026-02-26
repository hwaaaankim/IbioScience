package com.dev.IbioScience.dto.customer.auth;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawApproveBulkRequest {
	private List<Long> memberIds;
}