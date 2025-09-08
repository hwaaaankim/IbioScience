package com.dev.IbioScience.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.auth.CompanyProfile;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {
}