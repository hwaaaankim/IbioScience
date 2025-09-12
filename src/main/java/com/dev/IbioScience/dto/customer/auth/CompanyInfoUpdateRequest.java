package com.dev.IbioScience.dto.customer.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 기업회원 정보수정 요청 DTO - 비밀번호/확인은 모두 비어있으면 "비번 미변경" - username 변경 가능 (서버에서 중복검증) -
 * 지번주소는 선택
 */
@Data
public class CompanyInfoUpdateRequest {

	// ===== 공통 =====
	private Long memberId; // PathVariable과 중복 방지용(서버 검증용)

	@NotBlank(message = "아이디를 입력해 주세요.")
	@Pattern(regexp = "^[a-zA-Z0-9_\\-]{4,60}$", message = "아이디는 4~60자의 영문/숫자/특수문자(_,-)만 가능합니다.")
	private String username;

	@NotBlank(message = "이름을 입력해 주세요.")
	private String name;

	// 비밀번호(선택)
	private String password; // nullable
	private String passwordCheck; // nullable

	// 휴대폰(선택: 프론트에서 3-4-4 검증, 서버는 포맷 재조합만 수행)
	private String mobile1; // 3
	private String mobile2; // 4
	private String mobile3; // 4

	@NotBlank(message = "이메일을 입력해 주세요.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	private String email;

	// ===== 기본 주소 (지번 선택) =====
	@NotBlank(message = "우편번호를 입력해 주세요.")
	private String zip; // postcode

	@NotBlank(message = "도로명 주소를 입력해 주세요.")
	private String road;

	// 지번 선택
	private String jibun;

	@NotBlank(message = "상세 주소를 입력해 주세요.")
	private String detail;

	// ===== 회사(기업) =====
	@NotBlank(message = "기업명을 입력해 주세요.")
	private String companyName;

	@NotBlank(message = "대표자명을 입력해 주세요.")
	private String ceoName;

	private String department; // 선택
	private String companyEmail; // 선택

	private String bizType; // 업태 (선택)
	private String bizItem; // 종목 (선택)

	// 회사 전화/팩스 (선택)
	private String bizTel1;
	private String bizTel2;
	private String bizTel3;

	private String fax1;
	private String fax2;
	private String fax3;

	// 사업자등록번호 3-2-5
	@NotBlank(message = "사업자등록번호를 입력해 주세요.")
	private String bizNo1;
	@NotBlank(message = "사업자등록번호를 입력해 주세요.")
	private String bizNo2;
	@NotBlank(message = "사업자등록번호를 입력해 주세요.")
	private String bizNo3;

	// ===== 사업장 주소 (지번 선택) =====
	@NotBlank(message = "사업장 우편번호를 입력해 주세요.")
	private String workplaceZip;

	@NotBlank(message = "사업장 도로명 주소를 입력해 주세요.")
	private String workplaceRoad;

	// 선택
	private String workplaceJibun;

	@NotBlank(message = "사업장 상세 주소를 입력해 주세요.")
	private String workplaceDetail;

	/**
	 * 기존 사업자등록증 파일을 화면에서 X로 "삭제표시"했는지 여부. - true 이고 신규 업로드가 없다면 검증에 걸려야 함(최소 1개
	 * 규칙).
	 */
	private boolean deleteExistingBizReg;

	/**
	 * 프론트에서 "아이디 중복확인 완료" 시 true로 세팅. - username 변경이 없는 경우에도 true/false 무관하나, 변경이
	 * 있는 경우 반드시 true 여야 함.
	 */
	private boolean usernameDupChecked;
}