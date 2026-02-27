package com.dev.IbioScience.dto.customer.auth.transfer;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerDealerApproveRequest {

	private String processNote;

	private String shopName;
	private String tradingStatus;
	private String supplyType;
	private String supplyStructure;
	private String productTypeText;
	private String tel;
	private String fax;
	private String homepageUrl;

	private AddressDto businessAddress;
	private AddressDto returnAddress;

	private List<SellerContactDto> contacts;
	private List<CategoryPermissionDto> categoryPermissions;

	// ✅ 정산정책(추가)
	private SettlementPolicyDto settlementPolicy;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class AddressDto {
		private String postcode;
		private String roadAddress;
		private String jibunAddress;
		private String detailAddress;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class SellerContactDto {
		private String name;
		private String phone;
		private String email;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class CategoryPermissionDto {
		private Long largeId;
		private Long mediumId;
		private Long smallId;
	}

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class SettlementPolicyDto {
		private BigDecimal commissionRate; // 0~100
		private String cycle;              // SettlementCycle.name()
		private String basis;              // SettlementBasis.name()
	}
}