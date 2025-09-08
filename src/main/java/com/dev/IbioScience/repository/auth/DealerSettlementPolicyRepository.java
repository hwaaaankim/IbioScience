package com.dev.IbioScience.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.DealerSettlementPolicy;
import com.dev.IbioScience.model.auth.SellerDealerProfile;

public interface DealerSettlementPolicyRepository extends JpaRepository<DealerSettlementPolicy, Long> {
	Optional<DealerSettlementPolicy> findBySellerDealerProfile(SellerDealerProfile profile);
}