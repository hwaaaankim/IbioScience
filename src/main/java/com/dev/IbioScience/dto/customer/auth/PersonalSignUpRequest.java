package com.dev.IbioScience.dto.customer.auth;

import com.dev.IbioScience.model.auth.embedded.Address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PersonalSignUpRequest {

	/** 로그인 아이디 */
	@NotBlank
	@Size(max = 60)
	private String username;

	/** 암호(평문으로 들어와 BCrypt로 인코딩) */
	@NotBlank
	@Size(min = 5, max = 16)
	private String password;

	/** 이름 */
	@NotBlank
	@Size(max = 100)
	private String name;

	/** 유선전화 (예: 02-1234-5678) */
	@NotBlank
	@Size(max = 30)
	@Pattern(regexp = "^[0-9\\-]+$")
	private String tel;

	/** 휴대폰 (예: 010-1234-5678) */
	@NotBlank
	@Size(max = 30)
	@Pattern(regexp = "^[0-9\\-]+$")
	private String mobile;

	/** 이메일 */
	@NotBlank
	@Email
	@Size(max = 200)
	private String email;

	/** 주소 */
	private Address address; // address.postcode / roadAddress / detailAddress 로 바인딩

	/** 개인회원의 기관/업체명(선택 → 요구에 맞춰 필수로 처리) */
	@NotBlank
	@Size(max = 200)
	private String organizationName;

	/** 약관 동의(서버 기록용) */
	private boolean termsAgreed;
	private boolean privacyAgreed;
	private boolean smsAgreed;
	private boolean emailAgreed;
}