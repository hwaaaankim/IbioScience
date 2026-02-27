package com.dev.IbioScience.repository.auth.crm;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.SellerContact;

public interface CrmSellerContactRepository extends JpaRepository<SellerContact, Long> {
    List<SellerContact> findBySellerDealerProfile_IdOrderByIdAsc(Long sellerDealerProfileId);
}