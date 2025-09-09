package com.dev.IbioScience.dto.customer.auth;

import java.io.Serializable;

import org.springframework.web.multipart.MultipartFile;

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
public class CompanySignUpRequest implements Serializable {

	private static final long serialVersionUID = 2025L;

	// ===== Step1: 계정정보 =====
	@NotBlank
    @Size(min = 4, max = 60)
    private String username;

    @NotBlank
    @Size(min = 5, max = 64)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String name;

    /** 유선전화(완성형, 선택) */
    @Size(max = 30)
    private String tel;

    /** 휴대폰(완성형, 필수) */
    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^(01[0-9])-?[0-9]{3,4}-?[0-9]{4}$",
             message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String mobile;

    @NotBlank
    @Email
    @Size(max = 200)
    private String email;

    /** 주소(개인) */
    @Size(max = 10)
    private String aPostcode;
    @Size(max = 255)
    private String aRoadAddress;
    @Size(max = 255)
    private String aDetailAddress;

    // ===== Step2: 회사정보 =====
    @NotBlank @Size(max = 200)
    private String companyName;

    @Size(max = 100)
    private String department;

    @NotBlank @Size(max = 100)
    private String ceoName;

    @NotBlank @Size(max = 100)
    private String businessType;  // 업태

    @NotBlank @Size(max = 100)
    private String businessItem;  // 업종

    /** 회사 주소 */
    @NotBlank @Size(max = 10)
    private String cPostcode;
    @NotBlank @Size(max = 255)
    private String cRoadAddress;
    @Size(max = 255)
    private String cDetailAddress;

    /** 담당자 */
    @NotBlank @Size(max = 100)
    private String managerName;
    /** 담당자 연락처(완성형) */
    @NotBlank @Size(max = 30)
    private String managerPhone;

    /** 기관분류: GROUP_A/GROUP_B/GROUP_C */
    @NotBlank
    private String organizationCategory;

    // ===== 세금계산서 발행 정보 =====
    @NotBlank @Size(max = 30)
    private String businessRegistrationNumber;

    /** 회사 대표번호(완성형) */
    @Size(max = 30)
    private String representativeTel;

    /** 팩스(완성형) */
    @Size(max = 30)
    private String fax;

    @NotBlank
    @Email
    @Size(max = 200)
    private String invoiceEmail;

    /** 사업자등록증 파일(1개) */
    private MultipartFile bizRegFile;

    // ===== 약관 동의 =====
    private boolean agreeTerms;     // 필수
    private boolean agreePrivacy;   // 필수
    private boolean agreeSms;       // 선택
    private boolean agreeEmail;     // 선택
}