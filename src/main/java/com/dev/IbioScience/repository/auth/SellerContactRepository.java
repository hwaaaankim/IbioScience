package com.dev.IbioScience.repository.auth;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.SellerContact;
import com.dev.IbioScience.model.auth.SellerDealerProfile;

public interface SellerContactRepository extends JpaRepository<SellerContact, Long> {
	List<SellerContact> findBySellerDealerProfile(SellerDealerProfile profile);
}