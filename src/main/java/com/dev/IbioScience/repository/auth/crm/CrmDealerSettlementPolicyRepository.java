package com.dev.IbioScience.repository.auth.crm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.DealerSettlementPolicy;

public interface CrmDealerSettlementPolicyRepository extends JpaRepository<DealerSettlementPolicy, Long> {
    Optional<DealerSettlementPolicy> findBySellerDealerProfile_Id(Long sellerDealerProfileId);
}