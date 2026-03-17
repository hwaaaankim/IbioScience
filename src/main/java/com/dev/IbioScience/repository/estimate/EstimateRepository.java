package com.dev.IbioScience.repository.estimate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dev.IbioScience.model.estimate.Estimate;

public interface EstimateRepository extends JpaRepository<Estimate, Long>, JpaSpecificationExecutor<Estimate>, EstimateRepositoryCustom {

    List<Estimate> findAllByIdInAndMember_Id(Collection<Long> ids, Long memberId);

    Optional<Estimate> findByIdAndMember_Id(Long id, Long memberId);
    
    Optional<Estimate> findByIdAndMemberId(Long id, Long memberId);
    
}