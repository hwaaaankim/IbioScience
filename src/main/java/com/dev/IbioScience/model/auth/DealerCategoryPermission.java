package com.dev.IbioScience.model.auth;

import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 판매딜러 카테고리 권한: - large/medium/small 중 "하나"만 채우는 것을 서비스에서 보증. - large만 채운 경우:
 * 해당 대분류 이하 전부 가능 - medium만 채운 경우: 해당 중분류 이하 전부 가능 - small만 채운 경우: 해당 소분류만 가능
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dealer_category_permission", uniqueConstraints = {
		@UniqueConstraint(name = "uk_dcp_large", columnNames = { "seller_dealer_profile_id", "large_id" }),
		@UniqueConstraint(name = "uk_dcp_medium", columnNames = { "seller_dealer_profile_id", "medium_id" }),
		@UniqueConstraint(name = "uk_dcp_small", columnNames = { "seller_dealer_profile_id", "small_id" }) })
public class DealerCategoryPermission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 소속 판매딜러 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_dealer_profile_id", nullable = false)
	private SellerDealerProfile sellerDealerProfile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "large_id")
	private CategoryLarge large;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "medium_id")
	private CategoryMedium medium;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "small_id")
	private CategorySmall small;
}