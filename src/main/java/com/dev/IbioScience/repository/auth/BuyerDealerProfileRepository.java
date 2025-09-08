package com.dev.IbioScience.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.BuyerDealerProfile;
import com.dev.IbioScience.model.auth.Member;

public interface BuyerDealerProfileRepository extends JpaRepository<BuyerDealerProfile, Long> {
	Optional<BuyerDealerProfile> findByMember(Member member);
}