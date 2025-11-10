package com.dev.IbioScience.model.auth;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
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
 * 판매딜러 정산 정책
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dealer_settlement_policy", uniqueConstraints = @UniqueConstraint(name = "uk_settlement_seller", columnNames = "seller_dealer_profile_id"))
public class DealerSettlementPolicy extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 대상 판매딜러 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_dealer_profile_id", nullable = false)
	private SellerDealerProfile sellerDealerProfile;

	/** 수수료율(%) */
	@Column(precision = 5, scale = 2, nullable = false)
	private BigDecimal commissionRate;

	/** 정산주기(1/5/10/15/20/25/말일) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SettlementCycle cycle;

	/** 정산형태(결제완료/배송완료/구매확정완료) */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private SettlementBasis basis;

	/** 향후 스케줄링용(다음 정산 기준일) */
	private LocalDate nextSettlementDate;
}