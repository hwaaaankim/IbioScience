package com.dev.IbioScience.model.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * 판매딜러 담당자(여러 명)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seller_contact", indexes = @Index(name = "ix_seller_contact_seller", columnList = "seller_dealer_profile_id"))
public class SellerContact {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 소속 판매딜러 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_dealer_profile_id", nullable = false)
	private SellerDealerProfile sellerDealerProfile;

	/** 담당자명 */
	@Column(nullable = false, length = 100)
	private String name;

	/** 연락처 */
	@Column(length = 30)
	private String phone;

	/** 이메일 */
	@Column(length = 200)
	private String email;
}