package com.dev.IbioScience.dto.member.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffCreateRequest {

	@NotBlank
	private String role; // MASTER / OPERATOR / ADMIN

	@NotBlank
	private String name; // 담당자명

	@NotBlank
	@Size(min = 4, max = 30)
	private String username; // 로그인 아이디

	@NotBlank
	@Size(min = 5, max = 50)
	private String password; // 초기 비밀번호

	private String position; // 직급/직책 (빈 값 -> "-")
	private String tel; // 유선 (빈 값 -> "-")
	private String mobile; // 휴대폰 (빈 값 -> "-")
	private String email; // 이메일 (빈 값 -> "-")

	private Boolean isPrimary; // 대표 여부 (UI는 존재하지만 Member엔 필드 없음 -> 필요 시 확장)
	private Boolean useYn; // 사용여부 (default true)
}