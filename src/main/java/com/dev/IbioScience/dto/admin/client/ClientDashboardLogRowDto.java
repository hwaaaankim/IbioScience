package com.dev.IbioScience.dto.admin.client;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientDashboardLogRowDto {

	private String loggedAt; // yyyy-MM-dd HH:mm:ss
	private String username;
	private String name;

	private String customerType; // PERSONAL/BUSINESS/STAFF or "-"
	private String role;         // ROOT/MASTER/OPERATOR/ADMIN/USER...
	private String dealerType;   // NONE/BUYER/SELLER
	private String domain;       // CUSTOMER/COMPANY
}