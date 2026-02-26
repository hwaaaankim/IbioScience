package com.dev.IbioScience.dto.customer.auth;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WithdrawMemberDetailDto {

	// 공통(계정)
	private Long memberId;
	private String username;
	private String name;
	private String tel;
	private String mobile;
	private String email;

	private String customerType; // PERSONAL/BUSINESS
	private String dealerType;   // NONE/BUYER/SELLER
	private String status;       // WITHDRAWN 등

	private LocalDateTime joinedAt;
	private LocalDateTime withdrewAt;

	// 개인(표시용)
	private String organizationName;

	// 기업(CompanyProfile)
	private Long companyProfileId;
	private String companyName;
	private String department;
	private String ceoName;

	private String businessType;
	private String businessItem;

	private String representativeTel;
	private String fax;
	private String invoiceEmail;

	private String businessRegistrationNumber;

	// 회사 주소(CompanyProfile.companyAddress)
	private String companyPostcode;
	private String companyRoadAddress;
	private String companyDetailAddress;

	private String organizationCategory;

	// 사업자등록증 이미지(공개 URL)
	private String businessRegImageRoad;
}