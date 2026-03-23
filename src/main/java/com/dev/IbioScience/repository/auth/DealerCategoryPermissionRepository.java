package com.dev.IbioScience.repository.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
	
	@Query("""
        select d
        from DealerCategoryPermission d
        join fetch d.sellerDealerProfile s
        join fetch d.large l
        left join fetch d.medium m
        left join fetch d.small sm
        where s.id = :sellerProfileId
        order by l.name asc, m.name asc, sm.name asc
    """)
    List<DealerCategoryPermission> findAllWithCategoryBySellerProfileId(@Param("sellerProfileId") Long sellerProfileId);
	
	List<DealerCategoryPermission> findBySellerDealerProfileId(Long sellerDealerProfileId);
}