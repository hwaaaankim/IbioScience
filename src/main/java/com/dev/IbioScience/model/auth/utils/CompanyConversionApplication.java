package com.dev.IbioScience.model.auth.utils;

import java.time.LocalDateTime;

import com.dev.IbioScience.enums.auth.CompanyConversionStatus;
import com.dev.IbioScience.enums.product.OrganizationCategory;
import com.dev.IbioScience.model.auth.CompanyProfile;
import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.Address;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_conversion_application", indexes = {
	@Index(name = "ix_cca_applicant", columnList = "applicant_id"),
	@Index(name = "ix_cca_status", columnList = "status"),
	@Index(name = "ix_cca_requested_at", columnList = "requested_at")
})
public class CompanyConversionApplication {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 신청자 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "applicant_id", nullable = false)
	private Member applicant;

	/** 처리자(관리자) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "processor_id")
	private Member processor;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private CompanyConversionStatus status;

	@Column(name = "note", length = 1000)
	private String note;

	@Column(name = "process_note", length = 1000)
	private String processNote;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;

	@Column(name = "expired_at")
	private LocalDateTime expiredAt;

	// ===== 신청 회사정보(승인 전 임시 저장) =====

	@Column(name = "company_name", nullable = false, length = 200)
	private String companyName;

	@Column(name = "department", length = 100)
	private String department;

	@Column(name = "ceo_name", length = 100)
	private String ceoName;

	@Column(name = "business_type", length = 100)
	private String businessType;

	@Column(name = "business_item", length = 100)
	private String businessItem;

	@Column(name = "representative_tel", length = 30)
	private String representativeTel;

	@Column(name = "fax", length = 30)
	private String fax;

	@Column(name = "invoice_email", length = 200)
	private String invoiceEmail;

	@Column(name = "business_registration_number", length = 30)
	private String businessRegistrationNumber;

	@Column(name = "biz_reg_image_path", length = 500)
	private String businessRegImagePath;

	@Column(name = "biz_reg_image_road", length = 500)
	private String businessRegImageRoad;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "postcode", column = @Column(name = "c_postcode", length = 10)),
		@AttributeOverride(name = "roadAddress", column = @Column(name = "c_road_address", length = 255)),
		@AttributeOverride(name = "jibunAddress", column = @Column(name = "c_jibun_address", length = 255)),
		@AttributeOverride(name = "detailAddress", column = @Column(name = "c_detail_address", length = 255))
	})
	private Address companyAddress;

	@Enumerated(EnumType.STRING)
	@Column(name = "organization_category", length = 30)
	private OrganizationCategory organizationCategory;

	@Column(name = "homepage_url", length = 500)
	private String homepageUrl;

	/** 승인 후 실제 CompanyProfile 연결(추적용) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_company_profile_id")
	private CompanyProfile approvedCompanyProfile;
}