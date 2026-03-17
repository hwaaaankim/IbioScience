package com.dev.IbioScience.repository.estimate;

import org.springframework.data.domain.Page;

import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListRowDto;
import com.dev.IbioScience.dto.estimate.admin.AdminEstimateListSearchRequest;

public interface EstimateRepositoryCustom {

    Page<AdminEstimateListRowDto> searchAdminEstimateList(Long memberId, AdminEstimateListSearchRequest request);
}