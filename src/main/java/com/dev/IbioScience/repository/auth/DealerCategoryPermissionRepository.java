package com.dev.IbioScience.repository.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.DealerCategoryPermission;
import com.dev.IbioScience.model.auth.SellerDealerProfile;
import com.dev.IbioScience.model.product.category.CategoryLarge;
import com.dev.IbioScience.model.product.category.CategoryMedium;
import com.dev.IbioScience.model.product.category.CategorySmall;

public interface DealerCategoryPermissionRepository extends JpaRepository<DealerCategoryPermission, Long> {
	List<DealerCategoryPermission> findBySellerDealerProfile(SellerDealerProfile profile);

	boolean existsBySellerDealerProfileAndLarge(SellerDealerProfile profile, CategoryLarge large);

	boolean existsBySellerDealerProfileAndMedium(SellerDealerProfile profile, CategoryMedium medium);

	boolean existsBySellerDealerProfileAndSmall(SellerDealerProfile profile, CategorySmall small);
}