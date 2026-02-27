package com.dev.IbioScience.repository.auth.crm;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.DealerCategoryPermission;

public interface CrmDealerCategoryPermissionRepository extends JpaRepository<DealerCategoryPermission, Long> {

    List<DealerCategoryPermission> findBySellerDealerProfile_IdOrderByIdAsc(Long sellerDealerProfileId);

    Optional<DealerCategoryPermission> findByIdAndSellerDealerProfile_Id(Long id, Long sellerDealerProfileId);

    void deleteByIdAndSellerDealerProfile_Id(Long id, Long sellerDealerProfileId);
}