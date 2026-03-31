package com.dev.IbioScience.repository.settlement;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dev.IbioScience.enums.product.SettlementBasis;
import com.dev.IbioScience.enums.product.SettlementCycle;
import com.dev.IbioScience.model.settlement.DealerSettlementPolicyHistory;

public interface DealerSettlementPolicyHistoryRepository extends JpaRepository<DealerSettlementPolicyHistory, Long> {

    boolean existsBySellerDealerProfile_Id(Long sellerDealerProfileId);

    Optional<DealerSettlementPolicyHistory> findFirstBySellerDealerProfile_IdAndApplyEndDateIsNullOrderByIdDesc(
        Long sellerDealerProfileId
    );

    List<DealerSettlementPolicyHistory> findAllBySellerDealerProfile_IdOrderByApplyStartDateAscIdAsc(
        Long sellerDealerProfileId
    );

    @Query("""
        select h
        from DealerSettlementPolicyHistory h
        where h.sellerDealerProfile.id = :sellerDealerProfileId
          and h.applyStartDate <= :targetDate
          and (h.applyEndDate is null or h.applyEndDate >= :targetDate)
        order by h.applyStartDate desc, h.id desc
    """)
    List<DealerSettlementPolicyHistory> findEffectiveHistoriesAtDate(
        @Param("sellerDealerProfileId") Long sellerDealerProfileId,
        @Param("targetDate") LocalDate targetDate
    );

    @Query("""
        select h
        from DealerSettlementPolicyHistory h
        where h.sellerDealerProfile.id = :sellerDealerProfileId
          and h.applyStartDate > :targetDate
        order by h.applyStartDate asc, h.id asc
    """)
    List<DealerSettlementPolicyHistory> findScheduledHistoriesAfterDate(
        @Param("sellerDealerProfileId") Long sellerDealerProfileId,
        @Param("targetDate") LocalDate targetDate
    );

    @Query("""
        select distinct h
        from DealerSettlementPolicyHistory h
        join fetch h.sellerDealerProfile s
        join fetch s.member m
        left join fetch s.companyProfile cp
        where (:toDate is null or h.applyStartDate <= :toDate)
          and (:fromDate is null or h.applyEndDate is null or h.applyEndDate >= :fromDate)
          and (:cyclesEmpty = true or h.cycle in :cycles)
          and (:basesEmpty = true or h.basis in :bases)
          and (
                :keywordBlank = true
                or lower(coalesce(cp.companyName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(m.name, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(m.username, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(s.shopName, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(m.mobile, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(m.email, '')) like lower(concat('%', :keyword, '%'))
          )
        order by s.id asc, h.applyStartDate asc
    """)
    List<DealerSettlementPolicyHistory> searchForExecution(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("cycles") List<SettlementCycle> cycles,
        @Param("bases") List<SettlementBasis> bases,
        @Param("cyclesEmpty") boolean cyclesEmpty,
        @Param("basesEmpty") boolean basesEmpty,
        @Param("keywordBlank") boolean keywordBlank,
        @Param("keyword") String keyword
    );

    @Query("""
        select h
        from DealerSettlementPolicyHistory h
        join fetch h.sellerDealerProfile s
        join fetch s.member m
        left join fetch s.companyProfile cp
        where s.id in :sellerDealerProfileIds
          and (:toDate is null or h.applyStartDate <= :toDate)
          and (:fromDate is null or h.applyEndDate is null or h.applyEndDate >= :fromDate)
        order by s.id asc, h.applyStartDate asc, h.id asc
    """)
    List<DealerSettlementPolicyHistory> findAllForExecutionBySellerDealerProfileIds(
        @Param("sellerDealerProfileIds") Collection<Long> sellerDealerProfileIds,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
}