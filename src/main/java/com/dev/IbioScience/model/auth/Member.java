package com.dev.IbioScience.model.auth;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.auth.CustomerType;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.enums.auth.MemberDomain;
import com.dev.IbioScience.enums.auth.MemberRole;
import com.dev.IbioScience.enums.auth.MemberStatus;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원(회사/소비자 공통)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "member", uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_login_id", columnNames = "username") }, indexes = {
				@Index(name = "ix_member_email", columnList = "email"),
				@Index(name = "ix_member_role", columnList = "role"),
				@Index(name = "ix_member_status", columnList = "status") })
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 로그인 아이디(유니크) */
	@Column(name = "username", nullable = false, length = 60)
	private String username;

	/** 암호(BCrypt) */
	@Column(nullable = false, length = 100)
	private String password;

	/** 이름(담당자명) */
	@Column(nullable = false, length = 100)
	private String name;

	/** 유선전화 */
	@Column(length = 30)
	private String tel;

	/** 휴대폰 */
	@Column(length = 30)
	private String mobile;

	/** 이메일 */
	@Column(length = 200)
	private String email;
	
	@Column(name="point")
	private Long point;
	
	/** 기본 주소 (개인/기업 공통: 홈페이지 제외 모든 주소 포맷) */
	@Embedded
	private Address address;

	/** 소비자/회사 구분 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberDomain domain;

	 /**
     * 소비자 유형: 개인/기업
     * - domain == CUSTOMER 일 때만 유효
     * - STAFF인 경우 STAFF
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 20)
    private CustomerType customerType; // ✅ 신규
	
	/** 딜러 유형(NONE/BUYER/SELLER) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DealerType dealerType;

	/** 보안 권한 ROLE */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberRole role;

	/** 멤버 상태 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberStatus status;

	/** 기업회원인 경우 연결(회사정보 단일 모델) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_profile_id")
	private CompanyProfile companyProfile;

	/** 개인회원인 경우 입력할 수 있는 기관/업체명(간단 문자열) */
	@Column(length = 200)
	private String organizationName;

	/** 가입일 */
	private LocalDateTime joinedAt;

	/** 탈퇴일 */
	private LocalDateTime withdrewAt;

	/** 최초 로그인시 비번 변경 강제 여부(관리자 직접 생성 시 true) */
	@Column(nullable = false)
	private boolean mustChangePassword;

	/** 마지막 비번 변경 일시 */
	private LocalDateTime lastPasswordChangedAt;

	/** 직급(회사 내부용 등록 시) */
	@Column(length = 100)
	private String position;

	/** 사용여부 (회사 내부 등록폼 대응) */
	@Column(nullable = false)
	private boolean useYn;
	
	 /** ✅ 대표여부 (직원 목록에서 우선 표기/검색 용) */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary; // Lombok이 게터: isPrimary(), 세터: setPrimary(boolean) 생성
}