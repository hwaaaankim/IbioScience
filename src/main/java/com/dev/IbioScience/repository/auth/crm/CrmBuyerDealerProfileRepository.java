package com.dev.IbioScience.repository.auth.crm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.BuyerDealerProfile;

public interface CrmBuyerDealerProfileRepository extends JpaRepository<BuyerDealerProfile, Long> {
    Optional<BuyerDealerProfile> findByMember_Id(Long memberId);
}