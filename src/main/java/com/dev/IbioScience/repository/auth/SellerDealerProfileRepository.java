package com.dev.IbioScience.repository.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.SellerDealerProfile;

public interface SellerDealerProfileRepository extends JpaRepository<SellerDealerProfile, Long> {
	Optional<SellerDealerProfile> findByMember(Member member);
	boolean existsBySupplierCode(String supplierCode);
}