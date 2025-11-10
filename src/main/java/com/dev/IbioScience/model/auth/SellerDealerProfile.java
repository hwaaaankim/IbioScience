package com.dev.IbioScience.model.auth;

import java.time.LocalDate;

import com.dev.IbioScience.enums.product.SupplyStructure;
import com.dev.IbioScience.enums.product.SupplyType;
import com.dev.IbioScience.enums.product.TradingStatus;
import com.dev.IbioScience.model.auth.embedded.Address;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 판매딜러(입점) 프로필
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seller_dealer_profile", uniqueConstraints = @UniqueConstraint(name = "uk_seller_dealer_member", columnNames = "member_id"), indexes = @Index(name = "ix_seller_supplier_code", columnList = "supplier_code"))
public class SellerDealerProfile extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 대상 멤버(1:1) */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** 회사정보(중복 저장 방지, 공동사용) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "company_profile_id", nullable = false)
	private CompanyProfile companyProfile;

	/** 입점몰명 */
	@Column(nullable = false, length = 200)
	private String shopName;

	/** 로고 이미지 경로(path) */
	@Column(length = 500)
	private String logoImagePath;

	/** 로고 이미지 URL(road) */
	@Column(length = 500)
	private String logoImageRoad;

	/** 공급사 코드(랜덤/유니크) */
	@Column(name = "supplier_code", nullable = false, length = 40, unique = true)
	private String supplierCode;

	/** 거래상태(거래중/거래중지) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TradingStatus tradingStatus;

	/** 공급유형(사입/직배송) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SupplyType supplyType;

	/** 공급구조(전체/도매/사입/입점/기타) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SupplyStructure supplyStructure;

	/** 거래상품유형(텍스트) */
	@Column(length = 500)
	private String productTypeText;

	/** 일반전화 */
	@Column(length = 30)
	private String tel;

	/** 팩스번호 */
	@Column(length = 30)
	private String fax;

	/** 사업장 주소 */
	@Embedded
	private Address businessAddress;

	/** 반품 주소 */
	@AttributeOverrides({
			@AttributeOverride(name = "postcode", column = @Column(name = "return_postcode", length = 10)),
			@AttributeOverride(name = "roadAddress", column = @Column(name = "return_road_address", length = 255)),
			@AttributeOverride(name = "jibunAddress", column = @Column(name = "return_jibun_address", length = 255)),
			@AttributeOverride(name = "detailAddress", column = @Column(name = "return_detail_address", length = 255)) })
	private Address returnAddress;

	/** 홈페이지 URL */
	@Column(length = 500)
	private String homepageUrl;

	/** 거래 시작/중지 일자 */
	private LocalDate dealStartDate;
	private LocalDate dealStopDate;
}