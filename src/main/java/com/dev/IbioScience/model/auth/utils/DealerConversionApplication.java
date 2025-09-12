package com.dev.IbioScience.model.auth.utils;

import java.time.LocalDateTime;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.enums.CustomerType;
import com.dev.IbioScience.model.auth.enums.DealerType;

import jakarta.persistence.Column;
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
@Table(name = "dealer_conversion_application", indexes = {
		@Index(name = "ix_dca_applicant", columnList = "applicant_id"),
		@Index(name = "ix_dca_status", columnList = "status"),
		@Index(name = "ix_dca_to_type", columnList = "to_dealer_type") })
public class DealerConversionApplication {

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

	/** 신청 당시 보유 상태 */
	@Enumerated(EnumType.STRING)
	@Column(name = "from_dealer_type", nullable = false, length = 20)
	private DealerType fromDealerType;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_customer_type", nullable = false, length = 20)
	private CustomerType fromCustomerType;

	/** 전환 목표 */
	@Enumerated(EnumType.STRING)
	@Column(name = "to_dealer_type", nullable = false, length = 20)
	private DealerType toDealerType;

	/** 진행 상태 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private DealerApplicationStatus status;

	/** 특이사항(신청자 메모) */
	@Column(length = 1000)
	private String note;

	/** 관리자 처리 메모 */
	@Column(length = 1000)
	private String processNote;

	/** 타임스탬프 */
	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "processed_at")
	private LocalDateTime processedAt;
	
	@Column(name = "expired_at")
    private LocalDateTime expiredAt;
}