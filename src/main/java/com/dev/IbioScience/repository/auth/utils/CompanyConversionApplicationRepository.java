package com.dev.IbioScience.repository.auth.utils;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.auth.CompanyConversionStatus;
import com.dev.IbioScience.model.auth.utils.CompanyConversionApplication;

public interface CompanyConversionApplicationRepository extends JpaRepository<CompanyConversionApplication, Long> {

	boolean existsByApplicant_IdAndStatus(Long applicantId, CompanyConversionStatus status);

	Optional<CompanyConversionApplication> findTopByApplicant_IdAndStatusOrderByRequestedAtDesc(Long applicantId, CompanyConversionStatus status);

	List<CompanyConversionApplication> findAllByStatusOrderByRequestedAtDesc(CompanyConversionStatus status);
}