package com.dev.IbioScience.dto.customer.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerPersonalInfoUpdateRequest {

	@NotBlank
	private String id; // form hidden으로 전달 → Long 변환은 Controller/Service에서 처리

	@NotBlank
	@Size(min = 4, max = 30)
	private String username;

	@NotBlank
	@Size(min = 8, max = 32)
	private String password;

	@NotBlank
	private String name;

	// 휴대폰 3-4-4
	@NotBlank
	private String mobile1;
	@NotBlank
	private String mobile2;
	@NotBlank
	private String mobile3;

	// 사내번호(선택)
	private String tel1;
	private String tel2;
	private String tel3;

	// 주소
	@NotBlank
	private String zipcode;
	@NotBlank
	private String roadAddress;

	private String jibunAddress;
	
	@NotBlank
	private String detailAddress;

	// 이메일
	@NotBlank
	@Email
	private String email;

	// 소속(선택)
	private String organizationName;
}