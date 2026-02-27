package com.dev.IbioScience.repository.auth.crm;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.SellerDealerProfile;

public interface CrmSellerDealerProfileRepository extends JpaRepository<SellerDealerProfile, Long> {
    Optional<SellerDealerProfile> findByMember_Id(Long memberId);
}