package com.dev.IbioScience.dto.customer.auth.crm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSellerProfileRequest {

	private String shopName;
	private String supplierCode;
	private String tel;
	private String fax;
	private String homepageUrl;
	private String productTypeText;

	private String tradingStatus;
	private String supplyType;
	private String supplyStructure;

	private LocalDate dealStartDate;
	private LocalDate dealStopDate;

	/**
	 * KEEP / DELETE / REPLACE
	 */
	private String logoAction;

	private UpdateAddressPart businessAddress;
	private UpdateAddressPart returnAddress;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UpdateAddressPart {
		private String postcode;
		private String roadAddress;
		private String jibunAddress;
		private String detailAddress;
	}

	private SettlementPart settlement;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SettlementPart {
		private BigDecimal commissionRate;
		private String cycle;
		private String basis;
		private LocalDate nextSettlementDate;
	}

	private List<ContactItem> contacts;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ContactItem {
		private Long id;
		private String name;
		private String phone;
		private String email;
	}

	private List<AddPermissionItem> addPermissions;
	private List<Long> deletePermissionIds;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AddPermissionItem {
		private Long largeId;
		private Long mediumId;
		private Long smallId;
	}
}