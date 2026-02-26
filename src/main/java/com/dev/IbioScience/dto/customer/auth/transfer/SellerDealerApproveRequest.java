package com.dev.IbioScience.dto.customer.auth.transfer;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerDealerApproveRequest {

	private String processNote;

	// SellerDealerProfile 필수 입력
	private String shopName;

	/**
	 * 아래 3개는 엔티티 enum과 정확히 매칭되는 name() 문자열을 받습니다.
	 * (프론트는 /seller-enums 로 내려주는 값으로 셀렉트 구성)
	 */
	private String tradingStatus;
	private String supplyType;
	private String supplyStructure;

	private String productTypeText;
	private String tel;
	private String fax;
	private String homepageUrl;

	private AddressDto businessAddress;
	private AddressDto returnAddress;

	// 담당자(0~N)
	private List<SellerContactDto> contacts = new ArrayList<>();

	// 판매딜러 카테고리 권한(1개 이상 필수)
	private List<CategoryPermissionDto> categoryPermissions = new ArrayList<>();

	@Getter
	@Setter
	public static class AddressDto {
		private String postcode;
		private String roadAddress;
		private String jibunAddress;
		private String detailAddress;
	}

	@Getter
	@Setter
	public static class SellerContactDto {
		private String name;
		private String phone;
		private String email;
	}

	@Getter
	@Setter
	public static class CategoryPermissionDto {
		private Long largeId;
		private Long mediumId; // nullable
		private Long smallId;  // nullable
	}
}