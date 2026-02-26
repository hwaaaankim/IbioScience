package com.dev.IbioScience.repository.auth.utils;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.enums.auth.DealerApplicationStatus;
import com.dev.IbioScience.enums.auth.DealerType;
import com.dev.IbioScience.model.auth.utils.DealerConversionApplication;

public interface DealerConversionApplicationRepository extends JpaRepository<DealerConversionApplication, Long> {

	boolean existsByApplicant_IdAndStatusAndToDealerType(Long applicantId, DealerApplicationStatus status,
			DealerType toDealerType);

	List<DealerConversionApplication> findByApplicant_IdAndStatusAndToDealerType(Long applicantId,
			DealerApplicationStatus status, DealerType toDealerType);
}