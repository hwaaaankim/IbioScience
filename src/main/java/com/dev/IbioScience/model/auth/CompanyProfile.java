package com.dev.IbioScience.model.auth;

import com.dev.IbioScience.enums.product.OrganizationCategory;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회사(기업) 공통 정보 — 회원/판매딜러가 공유 사용
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_profile", indexes = { @Index(name = "ix_company_name", columnList = "company_name"),
		@Index(name = "ix_biz_no", columnList = "business_registration_number") })
public class CompanyProfile extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 업체명(상호) */
	@Column(name = "company_name", nullable = false, length = 200)
	private String companyName;

	/** 담당부서 */
	@Column(length = 100)
	private String department;

	/** 대표자명 */
	@Column(name = "ceo_name", length = 100)
	private String ceoName;

	/** 업태 */
	@Column(length = 100)
	private String businessType;

	/** 종목 */
	@Column(length = 100)
	private String businessItem;

	/** 회사 대표번호(계산서 대표번호로도 사용 가능) */
	@Column(length = 30)
	private String representativeTel;

	/** 팩스번호 */
	@Column(length = 30)
	private String fax;

	/** 계산서 발행용 이메일 */
	@Column(length = 200)
	private String invoiceEmail;

	/** 사업자등록번호 */
	@Column(name = "business_registration_number", length = 30)
	private String businessRegistrationNumber;

	/** 사업자등록증 파일 시스템 경로 */
	@Column(length = 500)
	private String businessRegImagePath;

	/** 사업자등록증 접근 URL(road) */
	@Column(length = 500)
	private String businessRegImageRoad;

	/** 회사 주소 */
	@Embedded
	private Address companyAddress;

	/** 기관 분류(임시 3개) */
	@Enumerated(EnumType.STRING)
	private OrganizationCategory organizationCategory;

	/** 홈페이지 URL (판매딜러 일반사항에 있으나 기업에도 보관 가능) */
	@Column(length = 500)
	private String homepageUrl;
}