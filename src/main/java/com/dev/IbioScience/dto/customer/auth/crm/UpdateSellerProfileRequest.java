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

	// ===== SellerDealerProfile editable fields =====
	private String shopName;
	private String tel;
	private String fax;
	private String homepageUrl;
	private String productTypeText;

	private String tradingStatus; // TradingStatus
	private String supplyType; // SupplyType
	private String supplyStructure; // SupplyStructure

	private LocalDate dealStartDate;
	private LocalDate dealStopDate;

	// ===== addresses =====
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

	// ===== settlement =====
	private SettlementPart settlement;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SettlementPart {
		private BigDecimal commissionRate;
		private String cycle; // SettlementCycle
		private String basis; // SettlementBasis
		private LocalDate nextSettlementDate;
	}

	// ===== category permissions =====
	private List<AddPermissionItem> addPermissions;
	private List<Long> deletePermissionIds;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AddPermissionItem {
		private Long largeId;
		private Long mediumId; // nullable
		private Long smallId; // nullable
	}
}