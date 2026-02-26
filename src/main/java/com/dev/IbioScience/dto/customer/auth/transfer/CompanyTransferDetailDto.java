package com.dev.IbioScience.dto.customer.auth.transfer;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CompanyTransferDetailDto {

	private Long applicationId;

	// 신청자(개인 계정)
	private Long memberId;
	private String username;
	private String name;
	private String mobile;
	private String email;

	// 신청 정보
	private String requestedAt;

	private String companyName;
	private String department;
	private String ceoName;
	private String businessType;
	private String businessItem;

	private String representativeTel;
	private String fax;
	private String invoiceEmail;
	private String businessRegistrationNumber;

	private String bizRegImageRoad; // 이미지 URL

	private String companyPostcode;
	private String companyRoadAddress;
	private String companyJibunAddress;
	private String companyDetailAddress;

	private String organizationCategory;
	private String homepageUrl;

	private String note;
}