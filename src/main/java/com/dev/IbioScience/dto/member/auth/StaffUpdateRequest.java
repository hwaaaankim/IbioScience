package com.dev.IbioScience.dto.member.auth;

import com.dev.IbioScience.enums.auth.MemberRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** 직원 상세 수정 요청 DTO */
@Getter
@Setter
@ToString
public class StaffUpdateRequest {

	@NotNull
	private Long id;

	// self만 수정 가능
	@Size(min = 4, max = 30)
	private String username;

	@Size(min = 1, max = 50)
	private String name;

	/** 비밀번호 미입력 시 변경 없음. 입력 시 유효성 최소 8자 권장 */
	@Size(min = 5, max = 100)
	private String password;

	// 선택
	private String position;

	private String tel; // "02-1234-5678"
	private String mobile; // "010-1234-5678"

	@Email
	private String email;

	// ROOT/MASTER만 수정 가능
	private MemberRole role; // "MASTER", "OPERATOR", "ADMIN" 등
	private Boolean useYn; // ROOT/MASTER만 반영
	private Boolean isPrimary; // ROOT/MASTER만 반영
}