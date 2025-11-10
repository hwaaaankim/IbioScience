package com.dev.IbioScience.model.auth;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.enums.auth.DealerGrade;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 구매딜러 프로필(개인/기업 공통)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "buyer_dealer_profile", uniqueConstraints = @UniqueConstraint(name = "uk_buyer_dealer_member", columnNames = "member_id"))
public class BuyerDealerProfile extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 대상 멤버(1:1) */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	/** 딜러 등급 (A/B/C/D/EXCEPTION/CUSTOM) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DealerGrade grade;

	/** 커스텀 할인율(grade=CUSTOM일 때 사용, % 단위 0~100) */
	@Column(precision = 5, scale = 2)
	private BigDecimal customDiscountRate;

	/** 적용 시작일 */
	private LocalDate effectiveFrom;
}