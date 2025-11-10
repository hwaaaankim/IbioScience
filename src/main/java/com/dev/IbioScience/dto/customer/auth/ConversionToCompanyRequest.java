package com.dev.IbioScience.dto.customer.auth;

import com.dev.IbioScience.enums.product.OrganizationCategory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversionToCompanyRequest {

	// 기업 기본
	@NotBlank
	@Size(max = 200)
	private String companyName;

	@Size(max = 100)
	private String department;

	@NotBlank
	@Size(max = 100)
	private String ceoName;

	@NotBlank
	@Size(max = 100)
	private String businessType;

	@NotBlank
	@Size(max = 100)
	private String businessItem;

	// 회사 주소
	@NotBlank
	private String cPostcode;

	@NotBlank
	private String cRoadAddress;

	@Size(max = 200)
	private String cDetailAddress;

	// 담당자/연락처
	@NotBlank
	@Size(max = 100)
	private String managerName;

	@NotBlank
	@Size(max = 30)
	private String managerPhone;

	// 기관 분류
	private OrganizationCategory organizationCategory;

	// 세금계산서
	@NotBlank
	@Size(max = 30)
	private String businessRegistrationNumber;

	@Size(max = 30)
	private String representativeTel;

	@Size(max = 30)
	private String fax;

	@NotBlank
	@Email
	@Size(max = 200)
	private String invoiceEmail;
}