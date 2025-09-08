package com.dev.IbioScience.model.auth;

import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;

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
 * 판매딜러 카테고리 권한(경로형)
 * - large(대분류): 필수
 * - medium(중분류): 선택(있으면 large의 하위여야 함)
 * - mediumSmall(중-소 매핑): 선택(있으면 medium과 일치해야 함)
 *
 * 유효한 조합은 3가지뿐:
 *  1) [large]
 *  2) [large, medium]
 *  3) [large, medium, mediumSmall]
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dealer_category_permission", indexes = {
		@Index(name = "ix_dcp_seller", columnList = "seller_dealer_profile_id"),
		@Index(name = "ix_dcp_large", columnList = "large_id"),
		@Index(name = "ix_dcp_medium", columnList = "medium_id"),
		@Index(name = "ix_dcp_small", columnList = "small_id"),
		@Index(name = "ix_dcp_seller_large", columnList = "seller_dealer_profile_id, large_id"),
		@Index(name = "ix_dcp_seller_large_medium", columnList = "seller_dealer_profile_id, large_id, medium_id") })
public class DealerCategoryPermission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 소속 판매딜러 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "seller_dealer_profile_id", nullable = false)
	private SellerDealerProfile sellerDealerProfile;

	/** 대분류(필수) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "large_id", nullable = false)
	private CategoryLarge large;

	/** 중분류(선택, 있으면 large의 하위여야 함) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "medium_id")
	private CategoryMedium medium;

	/** 소분류(선택, 있으면 medium의 하위여야 함) */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "small_id")
    private CategorySmall small;
}